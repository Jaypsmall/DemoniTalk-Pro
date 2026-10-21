pub mod normalizer;
pub mod wake_word;
pub mod fuzzy;
pub mod command_processor;

use std::ffi::{CStr, CString};
use std::os::raw::c_char;

use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;

use crate::command_processor::{process_command, VoiceCommand};
use crate::fuzzy::is_fuzzy_match;
use crate::normalizer::normalize;
use crate::wake_word::find_wake_word;

// ---------------------------------------------------------------------------
// JNI BINDINGS FOR KOTLIN / ANDROID
// Package: com.jaylizapp.demonitalk_pro
// Class: NativeEngine
// ---------------------------------------------------------------------------

#[no_mangle]
pub extern "system" fn Java_com_jaylizapp_demonitalk_1pro_NativeEngine_isEngineReady(
    _env: JNIEnv,
    _class: JClass,
) -> jni::sys::jboolean {
    1
}

#[no_mangle]
pub extern "system" fn Java_com_jaylizapp_demonitalk_1pro_NativeEngine_normalizeText(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    let input_str: String = match env.get_string(&input) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let normalized = normalize(&input_str);
    env.new_string(normalized)
        .unwrap_or_else(|_| env.new_string("").unwrap())
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_jaylizapp_demonitalk_1pro_NativeEngine_findWakeWord(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    let input_str: String = match env.get_string(&input) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let result_json = match find_wake_word(&input_str) {
        Some(w_match) => serde_json::to_string(&w_match).unwrap_or_default(),
        None => String::new(),
    };

    env.new_string(result_json)
        .unwrap_or_else(|_| env.new_string("").unwrap())
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_jaylizapp_demonitalk_1pro_NativeEngine_processCommand(
    mut env: JNIEnv,
    _class: JClass,
    input_text: JString,
    commands_json: JString,
    require_wake_word: jni::sys::jboolean,
) -> jstring {
    let text_str: String = match env.get_string(&input_text) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let json_str: String = match env.get_string(&commands_json) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let commands: Vec<VoiceCommand> = if !json_str.is_empty() {
        serde_json::from_str(&json_str).unwrap_or_default()
    } else {
        Vec::new()
    };

    let req_wake = require_wake_word != 0;
    let res = process_command(&text_str, &commands, req_wake);
    let out_json = serde_json::to_string(&res).unwrap_or_default();

    env.new_string(out_json)
        .unwrap_or_else(|_| env.new_string("").unwrap())
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_jaylizapp_demonitalk_1pro_NativeEngine_fuzzyMatch(
    mut env: JNIEnv,
    _class: JClass,
    text: JString,
    trigger: JString,
) -> jni::sys::jboolean {
    let text_str: String = match env.get_string(&text) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    let trigger_str: String = match env.get_string(&trigger) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    if is_fuzzy_match(&text_str, &trigger_str) { 1 } else { 0 }
}

// ---------------------------------------------------------------------------
// C FFI EXPORTS (FOR FLUTTER dart:ffi OR C INTEGRATION)
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn demoni_normalize(input: *const c_char) -> *mut c_char {
    if input.is_null() {
        return CString::new("").unwrap().into_raw();
    }
    let c_str = match CStr::from_ptr(input).to_str() {
        Ok(s) => s,
        Err(_) => "",
    };
    let normalized = normalize(c_str);
    CString::new(normalized).unwrap_or_default().into_raw()
}

#[no_mangle]
pub unsafe extern "C" fn demoni_find_wake_word(input: *const c_char) -> *mut c_char {
    if input.is_null() {
        return CString::new("").unwrap().into_raw();
    }
    let c_str = match CStr::from_ptr(input).to_str() {
        Ok(s) => s,
        Err(_) => "",
    };
    let json = match find_wake_word(c_str) {
        Some(m) => serde_json::to_string(&m).unwrap_or_default(),
        None => String::new(),
    };
    CString::new(json).unwrap_or_default().into_raw()
}

#[no_mangle]
pub unsafe extern "C" fn demoni_process_command(
    input_text: *const c_char,
    commands_json: *const c_char,
    require_wake_word: bool,
) -> *mut c_char {
    if input_text.is_null() {
        return CString::new("").unwrap().into_raw();
    }
    let text_str = match CStr::from_ptr(input_text).to_str() {
        Ok(s) => s,
        Err(_) => "",
    };

    let json_str = if commands_json.is_null() {
        ""
    } else {
        match CStr::from_ptr(commands_json).to_str() {
            Ok(s) => s,
            Err(_) => "",
        }
    };

    let commands: Vec<VoiceCommand> = if !json_str.is_empty() {
        serde_json::from_str(json_str).unwrap_or_default()
    } else {
        Vec::new()
    };

    let res = process_command(text_str, &commands, require_wake_word);
    let out_json = serde_json::to_string(&res).unwrap_or_default();
    CString::new(out_json).unwrap_or_default().into_raw()
}

#[no_mangle]
pub unsafe extern "C" fn demoni_free_string(s: *mut c_char) {
    if !s.is_null() {
        drop(CString::from_raw(s));
    }
}
