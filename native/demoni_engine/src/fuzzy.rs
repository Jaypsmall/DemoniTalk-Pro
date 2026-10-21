use std::cmp::min;

/// Calculates the Levenshtein distance between two strings.
pub fn levenshtein_distance(a: &str, b: &str) -> usize {
    let a_chars: Vec<char> = a.chars().collect();
    let b_chars: Vec<char> = b.chars().collect();

    let len_a = a_chars.len();
    let len_b = b_chars.len();

    if len_a == 0 { return len_b; }
    if len_b == 0 { return len_a; }

    let mut cache: Vec<usize> = (0..=len_b).collect();

    for (i, &ca) in a_chars.iter().enumerate() {
        let mut result = i + 1;
        let mut distance_b_zero = i;

        for (j, &cb) in b_chars.iter().enumerate() {
            let cost = if ca == cb { 0 } else { 1 };
            let distance_b_next = cache[j + 1];

            result = min(
                min(result + 1, cache[j] + cost),
                distance_b_zero + 1,
            );

            cache[j] = distance_b_zero;
            distance_b_zero = distance_b_next;
        }

        cache[len_b] = result;
    }

    cache[len_b]
}

/// Checks if a text fuzzy matches a target trigger command.
pub fn is_fuzzy_match(text: &str, trigger: &str) -> bool {
    let len_text = text.chars().count();
    let len_trig = trigger.chars().count();

    if len_text < 3 || len_trig < 3 {
        return false;
    }

    let diff = if len_text > len_trig {
        len_text - len_trig
    } else {
        len_trig - len_text
    };

    if diff > 5 {
        return false;
    }

    let prefix_len = ((len_trig as f32) * 0.65).floor() as usize;
    let prefix_len = prefix_len.max(1).min(len_trig);

    let trig_prefix: String = trigger.chars().take(prefix_len).collect();

    if text.contains(&trig_prefix) {
        return true;
    }

    let dist = levenshtein_distance(text, trigger);
    let max_allowed = (len_trig as f32 * 0.35).ceil() as usize;

    dist <= max_allowed.max(1)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_levenshtein() {
        assert_eq!(levenshtein_distance("camara", "camara"), 0);
        assert_eq!(levenshtein_distance("camara", "kamara"), 1);
    }

    #[test]
    fn test_fuzzy_match() {
        assert!(is_fuzzy_match("camara", "camara"));
        assert!(is_fuzzy_match("camar", "camara"));
    }
}
