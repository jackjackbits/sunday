import Foundation
import HealthKit

class HealthManager: ObservableObject {
    @Published var isAuthorized = false
    @Published var lastError: String?
    
    private let healthStore = HKHealthStore()
    private let vitaminDType = HKQuantityType.quantityType(forIdentifier: .dietaryVitaminD)!
    private let fitzpatrickSkinType = HKObjectType.characteristicType(forIdentifier: .fitzpatrickSkinType)!
    private let dateOfBirthType = HKObjectType.characteristicType(forIdentifier: .dateOfBirth)!
    
    init() {
        checkAuthorizationStatus()
    }
    
    func requestAuthorization() {
        guard HKHealthStore.isHealthDataAvailable() else {
            lastError = "Health data not available on this device"
            return
        }
        
        let typesToWrite: Set<HKSampleType> = [vitaminDType]
        let typesToRead: Set<HKObjectType> = [vitaminDType, fitzpatrickSkinType, dateOfBirthType]
        
        healthStore.requestAuthorization(toShare: typesToWrite, read: typesToRead) { [weak self] success, error in
            DispatchQueue.main.async {
                self?.isAuthorized = success
                self?.lastError = error?.localizedDescription
            }
        }
    }
    
    private func checkAuthorizationStatus() {
        let status = healthStore.authorizationStatus(for: vitaminDType)
        isAuthorized = status == .sharingAuthorized
    }
    
    func saveVitaminD(amount: Double, date: Date = Date()) {
      
        guard isAuthorized else {
            requestAuthorization()
        HapticManager.shared.vibrate(for: .error)
            return
        }
        HapticManager.shared.vibrate(for: .success)
        // Convert IU to micrograms (1 IU = 0.025 mcg)
        let micrograms = amount * 0.025
        let quantity = HKQuantity(unit: .gramUnit(with: .micro), doubleValue: micrograms)
        let sample = HKQuantitySample(
            type: vitaminDType,
            quantity: quantity,
            start: date,
            end: date,
            metadata: [
                HKMetadataKeyWasUserEntered: false,
                "Source": "Sun Day - UV Exposure",
                "Method": "Calculated from UV exposure",
                "OriginalValueInIU": amount
            ]
        )
        
        healthStore.save(sample) { [weak self] success, error in
            if let error = error {
                DispatchQueue.main.async {
                    self?.lastError = error.localizedDescription
                }
            }
        }
    }
    
    func getTodaysVitaminD(completion: @escaping (Double?) -> Void) {
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: Date())
        let endOfDay = calendar.date(byAdding: .day, value: 1, to: startOfDay)!
        
        let predicate = HKQuery.predicateForSamples(
            withStart: startOfDay,
            end: endOfDay,
            options: .strictStartDate
        )
        
        let query = HKStatisticsQuery(
            quantityType: vitaminDType,
            quantitySamplePredicate: predicate,
            options: .cumulativeSum
        ) { _, result, error in
            DispatchQueue.main.async {
                if let sum = result?.sumQuantity() {
                    // Convert micrograms back to IU (1 mcg = 40 IU)
                    let micrograms = sum.doubleValue(for: .gramUnit(with: .micro))
                    let iuValue = micrograms * 40.0
                    completion(iuValue)
                } else {
                    completion(nil)
                }
            }
        }
        
        healthStore.execute(query)
    }
    
    func getVitaminDHistory(days: Int, completion: @escaping ([Date: Double]) -> Void) {
        let calendar = Calendar.current
        let endDate = Date()
        guard let startDate = calendar.date(byAdding: .day, value: -days, to: endDate) else {
            completion([:])
            return
        }
        
        let predicate = HKQuery.predicateForSamples(
            withStart: startDate,
            end: endDate,
            options: .strictStartDate
        )
        
        var dailyTotals: [Date: Double] = [:]
        
        let query = HKSampleQuery(
            sampleType: vitaminDType,
            predicate: predicate,
            limit: HKObjectQueryNoLimit,
            sortDescriptors: [NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)]
        ) { _, samples, error in
            guard let samples = samples as? [HKQuantitySample], error == nil else {
                DispatchQueue.main.async {
                    completion([:])
                }
                return
            }
            
            // Group samples by day
            for sample in samples {
                let micrograms = sample.quantity.doubleValue(for: .gramUnit(with: .micro))
                let iuValue = micrograms * 40.0
                let dayStart = calendar.startOfDay(for: sample.startDate)
                
                dailyTotals[dayStart, default: 0] += iuValue
            }
            
            DispatchQueue.main.async {
                completion(dailyTotals)
            }
        }
        
        healthStore.execute(query)
    }
    
    func getFitzpatrickSkinType(completion: @escaping (HKFitzpatrickSkinType?) -> Void) {
        do {
            let skinType = try healthStore.fitzpatrickSkinType()
            DispatchQueue.main.async {
                completion(skinType.skinType)
            }
        } catch {
            DispatchQueue.main.async {
                self.lastError = error.localizedDescription
                completion(nil)
            }
        }
    }
    
    func getAge(completion: @escaping (Int?) -> Void) {
        do {
            let dateOfBirth = try healthStore.dateOfBirthComponents()
            let calendar = Calendar.current
            let now = Date()
            
            // Calculate age from date of birth
            if let birthDate = calendar.date(from: dateOfBirth) {
                let ageComponents = calendar.dateComponents([.year], from: birthDate, to: now)
                let age = ageComponents.year
                
                DispatchQueue.main.async {
                    completion(age)
                }
            } else {
                DispatchQueue.main.async {
                    completion(nil)
                }
            }
        } catch {
            DispatchQueue.main.async {
                self.lastError = error.localizedDescription
                completion(nil)
            }
        }
    }
    
    func readVitaminDIntake(from startDate: Date, to endDate: Date, completion: @escaping (Double, Error?) -> Void) {
        let predicate = HKQuery.predicateForSamples(
            withStart: startDate,
            end: endDate,
            options: .strictStartDate
        )
        
        let query = HKStatisticsQuery(
            quantityType: vitaminDType,
            quantitySamplePredicate: predicate,
            options: .cumulativeSum
        ) { _, result, error in
            DispatchQueue.main.async {
                if let sum = result?.sumQuantity() {
                    // Convert micrograms back to IU (1 mcg = 40 IU)
                    let micrograms = sum.doubleValue(for: .gramUnit(with: .micro))
                    let iuValue = micrograms * 40.0
                    completion(iuValue, error)
                } else {
                    completion(0.0, error)
                }
            }
        }
        
        healthStore.execute(query)
    }
}
