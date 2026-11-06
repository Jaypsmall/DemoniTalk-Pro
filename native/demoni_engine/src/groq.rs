use serde::{Deserialize, Serialize};
use std::error::Error;

#[derive(Serialize)]
pub struct Message {
    pub role: String,
    pub content: String,
}

#[derive(Serialize)]
pub struct GroqRequest {
    pub model: String,
    pub messages: Vec<Message>,
}

#[derive(Deserialize)]
pub struct GroqResponse {
    pub choices: Vec<Choice>,
}

#[derive(Deserialize)]
pub struct Choice {
    pub message: ResponseMessage,
}

#[derive(Deserialize)]
pub struct ResponseMessage {
    pub content: String,
}

/// Consulta a la API de Groq de forma síncrona con validación de estado HTTP
pub fn ask_groq(prompt: &str, api_key: &str) -> Result<String, Box<dyn Error>> {
    let client = reqwest::blocking::Client::builder()
        .timeout(std::time::Duration::from_secs(8))
        .build()?;

    let system_prompt = "Eres Demoni, un asistente de voz conciso en español. Responde en una o dos frases muy cortas para ser leídas por voz. NUNCA uses emojis.";

    let messages = vec![
        Message {
            role: "system".to_string(),
            content: system_prompt.to_string(),
        },
        Message {
            role: "user".to_string(),
            content: prompt.to_string(),
        },
    ];

    let request_body = GroqRequest {
        model: "llama-3.3-70b-versatile".to_string(),
        messages,
    };

    let response = client
        .post("https://api.groq.com/openai/v1/chat/completions")
        .bearer_auth(api_key)
        .json(&request_body)
        .send()?;

    // Validación vital: Si Groq devuelve un error (401, 429, 500, etc.), 
    // lo atrapamos y devolvemos el texto del error en lugar de hacer que crashee el JSON.
    if !response.status().is_success() {
        let status = response.status();
        let error_text = response.text().unwrap_or_else(|_| "Cuerpo de error ilegible".to_string());
        return Err(format!("Error de la API de Groq [{}]: {}", status, error_text).into());
    }

    let res: GroqResponse = response.json()?;

    if let Some(choice) = res.choices.into_iter().next() {
        Ok(choice.message.content)
    } else {
        Err("Groq API devolvió una respuesta vacía".into())
    }
}
