//
//  HapticManager.swift
//  Sunday
//
//  Created by Daniel Jermaine on 13/07/2025.
//

import Foundation

import UIKit

final class HapticManager {
   static   let shared = HapticManager()
    
    
    private init() {}
    
    public func vibrateForSelection() {
        DispatchQueue.main.async {
            let generator = UISelectionFeedbackGenerator()
            generator.prepare()
            generator.selectionChanged()
        }
    }
   
    
}
