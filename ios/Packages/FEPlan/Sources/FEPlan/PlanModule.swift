import Foundation
import SwiftData
import FECore
import FEData

@MainActor
public enum PlanModule {
    public static func makeComposer(supabase: FamilyEventsSupabase, modelContainer: ModelContainer) -> PlanComposer {
        #if canImport(CoreLocation) && canImport(WeatherKit)
        return PlanComposer(
            location: CoreLocationService(),
            weather: WeatherKitService(),
            planRepo: SupabasePlanRepository(supabase: supabase),
            eventRepo: SupabaseEventRepository(supabase: supabase),
            modelContainer: modelContainer
        )
        #else
        // Should never hit on iOS; included for macOS compilation safety.
        fatalError("PlanModule requires CoreLocation + WeatherKit")
        #endif
    }
}
