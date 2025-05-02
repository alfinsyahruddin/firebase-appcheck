use axum::{
    extract::Request,
    http::{HeaderMap, StatusCode},
    middleware::Next,
    response::IntoResponse,
    routing::get,
    Json, Router,
};
use jsonwebtoken::{decode, Algorithm, DecodingKey, Validation};
use serde::{Deserialize, Serialize};

const PROJECT_NUMBER: &str = "XXX";

#[tokio::main]
async fn main() {
    let app = Router::new()
        .route("/secret", get(secret))
        .layer(axum::middleware::from_fn(middleware_verify_app_check_token)); // Apply middleware

    println!("🚀 Server running on http://localhost:8000");
    let listener = tokio::net::TcpListener::bind("0.0.0.0:8000").await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn secret() -> impl IntoResponse {
    Json(SecretResponse {
        secret: "123456".to_string(),
    })
}

async fn middleware_verify_app_check_token(request: Request, next: Next) -> impl IntoResponse {
    let headers: &HeaderMap = request.headers();

    let Some(token) = headers
        .get("X-Firebase-AppCheck")
        .and_then(|hv| hv.to_str().ok())
    else {
        return (StatusCode::UNAUTHORIZED, "Missing App Check token").into_response();
    };

    match verify_token(token).await {
        Some(app_id) => {
            let mut req = request;
            req.extensions_mut().insert(app_id); // Attach app ID to request
            next.run(req).await
        }
        None => (StatusCode::UNAUTHORIZED, "Invalid App Check token").into_response(),
    }
}

/// Fetch Firebase App Check Public Keys (JWKS)
async fn fetch_jwks() -> Result<Jwks, reqwest::Error> {
    let response = reqwest::get("https://firebaseappcheck.googleapis.com/v1/jwks")
        .await?
        .text()
        .await?;
    let jwks = serde_json::from_str(&response).unwrap();
    Ok(jwks)
}

async fn verify_token(token: &str) -> Option<String> {
    let jwks = fetch_jwks().await.ok()?;

    let decoding_keys: Vec<DecodingKey> = jwks
        .keys
        .into_iter()
        .filter_map(|jwk| DecodingKey::from_rsa_components(&jwk.n, &jwk.e).ok())
        .collect();

    let mut validation = Validation::new(Algorithm::RS256);
    validation.set_audience(&[format!("projects/{PROJECT_NUMBER}")]);
    validation.set_issuer(&[format!(
        "https://firebaseappcheck.googleapis.com/{PROJECT_NUMBER}"
    )]);

    for key in decoding_keys {
        let result = decode::<Claims>(token, &key, &validation);
        match result {
            Ok(token_data) => {
                println!("Token data: {:?}", token_data);
                return Some(token_data.claims.sub); // Return App ID (sub)
            }
            Err(e) => {
                println!("Error: {:?}", e);
            }
        }
    }

    None
}

#[derive(Debug, Serialize, Deserialize)]
struct Jwks {
    keys: Vec<Jwk>,
}

#[derive(Debug, Serialize, Deserialize)]
struct Jwk {
    kty: String,
    kid: String,
    alg: String,
    n: String,
    e: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct Claims {
    iss: String,
    aud: Vec<String>,
    sub: String,
    exp: usize,
}

#[derive(Serialize)]
struct SecretResponse {
    secret: String,
}
