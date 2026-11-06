use crate::normalizer::normalize;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct WakeWordMatch {
    pub wake_word: String,
    pub remaining_text: String,
}

static WAKE_WORDS: &[&str] = &["demoni", "demonio", "hermano", "amigo", "maquina"];

/// Detects if a wake word is present at the beginning or within the normalized text.
pub fn find_wake_word(text: &str) -> Option<WakeWordMatch> {
    let normalized = normalize(text);
    if normalized.is_empty() {
        return None;
    }

    for &wake in WAKE_WORDS {
        if normalized == wake {
            return Some(WakeWordMatch {
                wake_word: wake.to_string(),
                remaining_text: String::new(),
            });
        }

        let prefix = format!("{} ", wake);
        if normalized.starts_with(&prefix) {
            let remaining = normalized[prefix.len()..].trim().to_string();
            return Some(WakeWordMatch {
                wake_word: wake.to_string(),
                remaining_text: remaining,
            });
        }
    }

    None
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_find_wake_word() {
        let res1 = find_wake_word("Demoni abre la camara");
        assert!(res1.is_some());
        let m1 = res1.unwrap();
        assert_eq!(m1.wake_word, "demoni");
        assert_eq!(m1.remaining_text, "abre la camara");

        let res2 = find_wake_word("Hola que tal");
        assert!(res2.is_none());
    }
}
