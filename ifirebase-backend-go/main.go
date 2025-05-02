package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"
	"sync"

	firebase "firebase.google.com/go/v4"
	"google.golang.org/api/option"
)

var (
	firebaseApp *firebase.App
	once        sync.Once
)

// Initialize Firebase App (singleton)
func getFirebaseApp() *firebase.App {
	once.Do(func() {
		ctx := context.Background()
		app, err := firebase.NewApp(ctx, nil, option.WithCredentialsFile("./serviceAccountKey.json"))
		if err != nil {
			log.Fatalf("🔥 Failed to initialize Firebase: %v", err)
		}
		firebaseApp = app
	})
	return firebaseApp
}

// Middleware to verify Firebase App Check token
func verifyAppCheck(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		token := strings.TrimSpace(r.Header.Get("X-Firebase-AppCheck"))
		if token == "" {
			http.Error(w, "🚫 Missing App Check token", http.StatusUnauthorized)
			return
		}

		appCheck, err := getFirebaseApp().AppCheck(r.Context())
		if err != nil {
			http.Error(w, "🚫 Failed to initialize App Check", http.StatusInternalServerError)
			return
		}

		result, err := appCheck.VerifyToken(token)
		if err != nil {
			http.Error(w, "🚫 Invalid App Check token", http.StatusUnauthorized)
			return
		}

		fmt.Println("✅ Verified App Check Token for App ID:", result.AppID)
		next.ServeHTTP(w, r)
	})
}

// Protected route
func secret(w http.ResponseWriter, r *http.Request) {
	json.NewEncoder(w).Encode(map[string]string{"secret": "123456"})
}

func main() {
	http.Handle("/secret", verifyAppCheck(http.HandlerFunc(secret)))
	fmt.Println("🚀 Server running on http://localhost:8000")
	log.Fatal(http.ListenAndServe(":8000", nil))
}
