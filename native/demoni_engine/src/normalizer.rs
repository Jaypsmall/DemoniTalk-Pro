use unicode_normalization::UnicodeNormalization;
use regex::Regex;
use std::sync::OnceLock;

static NON_ALPHANUMERIC_REGEX: OnceLock<Regex> = OnceLock::new();
static WHITESPACE_REGEX: OnceLock<Regex> = OnceLock::new();
static ALIAS_REGEXES: OnceLock<Vec<Regex>> = OnceLock::new();

fn get_non_alphanumeric_regex() -> &'static Regex {
    NON_ALPHANUMERIC_REGEX.get_or_init(|| {
        Regex::new(r"[^a-z0-9]+").unwrap()
    })
}

fn get_whitespace_regex() -> &'static Regex {
    WHITESPACE_REGEX.get_or_init(|| {
        Regex::new(r"\s+").unwrap()
    })
}

fn get_alias_regexes() -> &'static Vec<Regex> {
    ALIAS_REGEXES.get_or_init(|| {
        let aliases = vec![
            "demonio",
            "de money",
            "de moni",
            "d money",
            "demoni",
            "demony",
            "demon y",
            "demon",
            "maquina",
            "hermano",
            "amigo",
        ];

        aliases
            .into_iter()
            .map(|alias| {
                let escaped = regex::escape(alias);
                Regex::new(&format!(r"(?<!\S){}(?!\S)", escaped)).unwrap()
            })
            .collect()
    })
}

/// Normalizes voice input text by:
/// 1. Removing diacritical marks (NFD decomposition).
/// 2. Converting to lowercase.
/// 3. Replacing non-alphanumeric characters with spaces.
/// 4. Replacing recognized demoni aliases with "demoni".
/// 5. Collapsing multiple spaces.
pub fn normalize(text: &str) -> String {
    if text.is_empty() {
        return String::new();
    }

    // NFD normalization and diacritic removal
    let nfd_str: String = text.nfd().collect();
    let without_diacritics: String = nfd_str
        .chars()
        .filter(|c| !is_combining_mark(*c))
        .collect();

    let lowercase = without_diacritics.to_lowercase();

    // Replace non-alphanumeric characters
    let cleaned = get_non_alphanumeric_regex().replace_all(&lowercase, " ");
    let trimmed = cleaned.trim();
    let collapsed = get_whitespace_regex().replace_all(trimmed, " ");

    let mut result = collapsed.to_string();

    // Replace aliases with 'demoni'
    for alias_regex in get_alias_regexes() {
        result = alias_regex.replace_all(&result, "demoni").to_string();
    }

    get_whitespace_regex().replace_all(result.trim(), " ").to_string()
}

fn is_combining_mark(c: char) -> bool {
    ('\u{0300}'..='\u{036F}').contains(&c)
        || ('\u{1DC0}'..='\u{1DFF}').contains(&c)
        || ('\u{20D0}'..='\u{20FF}').contains(&c)
        || ('\u{FE20}'..='\u{FE2F}').contains(&c)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_normalize() {
        assert_eq!(normalize("Hola Demonio, abre CÁMARA!"), "hola demoni abre camara");
        assert_eq!(normalize("De Money pásame el link"), "demoni pasame el link");
        assert_eq!(normalize("  Hermano...   abre whatsapp "), "demoni abre whatsapp");
    }
}
