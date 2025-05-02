//
//  ViewController.swift
//  iFirebaseIOS
//
//  Created by M Alfin Syahruddin on 07/03/25.
//


import UIKit
import Alamofire
import FirebaseCore
import FirebaseAppCheck

class ViewController: UIViewController {
    @IBOutlet weak var loadingIndicator: UIActivityIndicatorView!
    
    @IBAction func didTapGetSecretButton(_ sender: Any) {
        loadingIndicator.startAnimating()
        AppCheck.appCheck().token(forcingRefresh: true) { token, error in
            if let error {
                print("[AppCheck] token error: \(error.localizedDescription)")
                self.loadingIndicator.stopAnimating()
                return
            }
            
            if let token = token?.token {
                print("[AppCheck] token: \(token)")
                
                AF.request(
                    "http://localhost:8000/secret",
                    headers: HTTPHeaders(["X-Firebase-AppCheck": token])
                ).responseDecodable(of: SecretResponse.self) { response in
                    switch response.result {
                    case .success(let data):
                        self.alert("✅ Success", "Secret: \(data.secret)")
                    case .failure(let error):
                        self.alert("❌ Failed", error.localizedDescription)
                    }
                    self.loadingIndicator.stopAnimating()
                }
            }
        }
    }
    
}

struct SecretResponse: Codable {
    var secret: String
}

extension UIViewController {
    func alert(_ title: String, _ message: String) {
        let ac = UIAlertController(title: title, message: message, preferredStyle: .alert)
        ac.addAction(UIAlertAction(title: "OK", style: .default))
        self.present(ac, animated: true)
    }
}

class FirebaseAppCheckProvider: NSObject, AppCheckProviderFactory {
  func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
    return AppAttestProvider(app: app)
  }
}
