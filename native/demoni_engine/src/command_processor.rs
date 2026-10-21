use crate::fuzzy::is_fuzzy_match;
use crate::normalizer::normalize;
use crate::wake_word::find_wake_word;
use regex::Regex;
use serde::{Deserialize, Serialize};
use std::sync::OnceLock;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VoiceCommand {
    pub trigger: String,
    pub action: String,
    #[serde(default)]
    pub is_root: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProcessResult {
    pub status: String, // "EXECUTIVE", "WAKE_WORD_ONLY", "IGNORED"
    pub actions: Vec<String>,
    pub wake_word_detected: bool,
    pub detected_wake_word: Option<String>,
}

static SEQUENCE_REGEX: OnceLock<Regex> = OnceLock::new();
static NUMBER_WORD_REGEXES: OnceLock<Vec<(Regex, &'static str)>> = OnceLock::new();
static CLICK_NUMBER_REGEX: OnceLock<Regex> = OnceLock::new();

fn get_sequence_regex() -> &'static Regex {
    SEQUENCE_REGEX.get_or_init(|| {
        Regex::new(
            r"\s*[*;,]\s*|\s+(?:luego|después|despues|entonces)\s+(?:el\s+|la\s+|los\s+|las\s+|un\s+|una\s+)?|\s+y\s+(?:el\s+|la\s+|los\s+|las\s+|un\s+|una\s+)?(?=abre|abrir|abreme|pulsa|clic|click|escribe|manda|enviar|pon|reproduce|vuelve|atras|inicio|casa|recientes|buscar|busca|primer|primera|chat|cerrar|activar|desactivar|activa|modo|silencio|detente|cuadricula|numeros|oculta|foco|camara|\*)"
        ).unwrap()
    })
}

fn get_number_word_regexes() -> &'static Vec<(Regex, &'static str)> {
    NUMBER_WORD_REGEXES.get_or_init(|| {
        vec![
            (Regex::new(r"\buno\b").unwrap(), "1"),
            (Regex::new(r"\bdos\b").unwrap(), "2"),
            (Regex::new(r"\btres\b").unwrap(), "3"),
            (Regex::new(r"\bcuatro\b").unwrap(), "4"),
            (Regex::new(r"\bcinco\b").unwrap(), "5"),
            (Regex::new(r"\bseis\b").unwrap(), "6"),
            (Regex::new(r"\bsiete\b").unwrap(), "7"),
            (Regex::new(r"\bocho\b").unwrap(), "8"),
            (Regex::new(r"\bnueve\b").unwrap(), "9"),
            (Regex::new(r"\bdiez\b").unwrap(), "10"),
        ]
    })
}

fn get_click_number_regex() -> &'static Regex {
    CLICK_NUMBER_REGEX.get_or_init(|| {
        Regex::new(r"(?:pulsa|pulsar|clic|click|numero|el|toca|tocar|presiona|presionar|selecciona|seleccionar|opcion)\s+(?:el\s+)?(\d+)").unwrap()
    })
}

/// Main entry point to process voice input using Rust engine.
pub fn process_command(
    text: &str,
    commands: &[VoiceCommand],
    require_wake_word: bool,
) -> ProcessResult {
    let mut normalized = normalize(text);
    let mut wake_word_detected = false;
    let mut detected_wake_word = None;

    if require_wake_word {
        if let Some(w_match) = find_wake_word(&normalized) {
            wake_word_detected = true;
            detected_wake_word = Some(w_match.wake_word);
            normalized = w_match.remaining_text;
        } else {
            return ProcessResult {
                status: "IGNORED".to_string(),
                actions: Vec::new(),
                wake_word_detected: false,
                detected_wake_word: None,
            };
        }
    }

    if wake_word_detected && normalized.is_empty() {
        return ProcessResult {
            status: "WAKE_WORD_ONLY".to_string(),
            actions: Vec::new(),
            wake_word_detected: true,
            detected_wake_word,
        };
    }

    let parts: Vec<String> = get_sequence_regex()
        .split(&normalized)
        .map(|s| s.trim().trim_start_matches(|c| c == '*' || c == ';' || c == ',' || c == ' ').trim().to_string())
        .filter(|s| !s.is_empty())
        .collect();

    let mut actions = Vec::new();

    if parts.len() > 1 {
        for part in parts {
            if let Some(acts) = parse_single_command(&part, commands) {
                actions.extend(acts);
            }
        }
    } else if let Some(acts) = parse_single_command(&normalized, commands) {
        actions.extend(acts);
    }

    if !actions.is_empty() {
        ProcessResult {
            status: "EXECUTIVE".to_string(),
            actions,
            wake_word_detected,
            detected_wake_word,
        }
    } else {
        ProcessResult {
            status: "IGNORED".to_string(),
            actions: Vec::new(),
            wake_word_detected,
            detected_wake_word,
        }
    }
}

fn parse_single_command(text: &str, commands: &[VoiceCommand]) -> Option<Vec<String>> {
    let mut clean_text = normalize(text);
    clean_text = clean_text.trim_start_matches(|c| c == '*' || c == ';' || c == ',' || c == ' ').trim().to_string();

    if clean_text.is_empty() {
        return None;
    }

    let is_first_chat = clean_text.contains("primer chat")
        || clean_text.contains("primera conversacion")
        || clean_text.contains("primer contacto")
        || clean_text.contains("primer mensaje")
        || clean_text.contains("primera charla");

    if is_first_chat {
        let mentions_wa = clean_text.contains("whatsapp")
            || clean_text.contains("wasap")
            || clean_text.contains("wasat")
            || clean_text.contains("guatsap");

        return if mentions_wa {
            Some(vec!["open_app:whatsapp".to_string(), "click_first_chat".to_string()])
        } else {
            Some(vec!["click_first_chat".to_string()])
        };
    }

    if let Some(rest) = clean_text.strip_prefix("escribe ") {
        if !rest.trim().is_empty() {
            return Some(vec![format!("type:{}", rest.trim())]);
        }
    }

    if let Some(rest) = clean_text.strip_prefix("manda este mensaje ") {
        if !rest.trim().is_empty() {
            return Some(vec![format!("type:{}", rest.trim())]);
        }
    }

    fn extract_clean_app(t: &str) -> String {
        let mut name = t.trim();
        for conn in &[" para ", " de ", " en ", " y "] {
            if let Some(pos) = name.find(conn) {
                name = &name[..pos].trim();
            }
        }
        name.to_string()
    }

    for prefix in &["abre ", "abreme ", "abrir ", "abrirme "] {
        if let Some(rest) = clean_text.strip_prefix(prefix) {
            let app = extract_clean_app(rest);
            if !app.is_empty() {
                return Some(vec![format!("open_app:{}", app)]);
            }
        }
    }

    match clean_text.as_str() {
        "vuelve atras" | "atras" | "volver atras" => return Some(vec!["global_back".to_string()]),
        "inicio" | "vete a casa" | "ir a casa" | "casa" => return Some(vec!["global_home".to_string()]),
        "recientes" | "aplicaciones recientes" => return Some(vec!["global_recents".to_string()]),
        "enviar" | "manda el mensaje" | "enviar mensaje" => return Some(vec!["click_send".to_string()]),
        "cuadricula" | "cuadricula de numeros" | "muestra cuadricula" => return Some(vec!["show_grid".to_string()]),
        "numeros" | "muestra numeros" | "mostrar numeros" | "actualizar" | "actualizar numeros" | "refrescar" | "refrescar numeros" => return Some(vec!["show_numbers".to_string()]),
        "oculta todo" | "limpia pantalla" | "ocultar todo" => return Some(vec!["hide_overlays".to_string()]),
        "silencio" | "para de escuchar" | "deja de escuchar" | "deja de oir" | "detente" => return Some(vec!["internal_stop".to_string()]),
        "activa c4" | "activa c 4" | "activar c4" | "activar c 4" | "modo azul" | "modo vigilancia" | "activar vigilancia" => return Some(vec!["internal_mode_blue".to_string()]),
        "activar escucha" | "activa escucha" | "modo verde" | "modo continuo" | "escucha continua" => return Some(vec!["internal_continuous_on".to_string()]),
        "siguiente cancion" | "siguiente" | "proxima" => return Some(vec!["media_next".to_string()]),
        "cancion anterior" | "anterior" => return Some(vec!["media_previous".to_string()]),
        "para la musica" | "para" | "pausa" | "deten la musica" => return Some(vec!["media_pause".to_string()]),
        "reanuda la musica" | "continua" | "play" => return Some(vec!["media_play".to_string()]),
        _ => {}
    }

    if clean_text.starts_with("reproduce ") || clean_text.starts_with("pon ") {
        let query = clean_text
            .trim_start_matches("reproduce ")
            .trim_start_matches("pon ")
            .trim();
        if !query.is_empty() {
            return Some(vec![format!("play_music:{}", query)]);
        }
    }

    if clean_text.starts_with("busca ") || clean_text.starts_with("buscar ") {
        let query = clean_text
            .trim_start_matches("busca ")
            .trim_start_matches("buscar ")
            .trim();
        if !query.is_empty() {
            return Some(vec![format!("search_web:{}", query)]);
        }
    }

    // Number conversions
    let mut num_text = clean_text.clone();
    for (re, replacement) in get_number_word_regexes() {
        num_text = re.replace_all(&num_text, *replacement).to_string();
    }

    if let Some(caps) = get_click_number_regex().captures(&num_text) {
        if let Some(num_match) = caps.get(1) {
            return Some(vec![format!("click_number:{}", num_match.as_str())]);
        }
    }

    if num_text.chars().all(|c| c.is_ascii_digit()) && !num_text.is_empty() {
        return Some(vec![format!("click_number:{}", num_text)]);
    }

    for prefix in &["pulsa ", "toca ", "presiona "] {
        if let Some(target) = clean_text.strip_prefix(prefix) {
            let t = target.trim();
            if !t.is_empty() {
                return Some(vec![format!("click_text:{}", t)]);
            }
        }
    }

    // Check user defined commands
    for cmd in commands {
        let trigger_norm = normalize(&cmd.trigger);
        if clean_text == trigger_norm || clean_text.contains(&trigger_norm) || is_fuzzy_match(&clean_text, &trigger_norm) {
            return Some(vec![cmd.action.clone()]);
        }
    }

    None
}
