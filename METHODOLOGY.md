# Sun Day - Vitamin D Calculation Methodology

## Overview

Sun Day calculates vitamin D synthesis from UV exposure using a multi-factor model based on scientific research. The app aims to provide personalized, accurate estimates while remaining conservative for safety. See [citations](https://github.com/jackjackbits/sunday/blob/main/METHODOLOGY.md#references).

## Core Formula

```
Vitamin D Rate (IU/hour) = Base Rate × UV Factor × Clothing Factor × Skin Type Factor × Age Factor × Quality Factor × Adaptation Factor
```

## Factor Breakdown

### 1. Base Rate (21,000 IU/hr)
- Represents minimal clothing exposure (~80% body surface area)
- Conservative estimate within research range of 20,000-40,000 IU/hr
- Studies show 10,000 IU in 20-30 minutes typical
- Full body exposure can reach 30,000-40,000 IU/hr in optimal conditions

### 2. UV Factor (Non-linear)
- Implements Michaelis-Menten-like saturation curve
- Formula: `uvFactor = (uvIndex × 3.0) / (4.0 + uvIndex)`
- Accounts for:
  - Vitamin D synthesis plateaus at high UV levels
  - Photodegradation of vitamin D above UV ~8
  - Limited 7-dehydrocholesterol in skin

### 3. Clothing Factor
- **Nude (100%)**: Full body exposure
- **Minimal/Swimwear (80%)**: Typical beach attire
- **Light/Shorts & T-shirt (40%)**: Summer casual wear
- **Moderate/Long sleeves (15%)**: Business casual
- **Heavy/Fully covered (5%)**: Winter clothing

### 4. Skin Type Factor (Fitzpatrick Scale)
- **Type I (125%)**: Very fair, always burns - highest vitamin D production
- **Type II (110%)**: Fair, usually burns
- **Type III (100%)**: Light, sometimes burns - reference type
- **Type IV (70%)**: Medium, rarely burns
- **Type V (40%)**: Dark, very rarely burns  
- **Type VI (20%)**: Very dark, never burns

Based on melanin's UV filtering effect and research showing 5-10x longer exposure needed for darker skin types.

### 5. Age Factor
- **≤20 years**: 100% efficiency
- **20-70 years**: Linear decrease (~1% per year)
- **≥70 years**: 25% efficiency

Reflects decreased 7-dehydrocholesterol in aging skin.

### 6. UV Quality Factor (Time of Day)
- Accounts for solar zenith angle effects on UV-B transmission
- Peak quality around solar noon (10 AM - 3 PM)
- More gradual decrease at low sun angles (exp(-0.2) vs exp(-0.3))
- Morning/evening UV has less effective UV-B wavelengths

### 7. Adaptation Factor
- Based on 7-14 day exposure history from HealthKit
- Range: 0.8-1.2x
- Regular exposure upregulates vitamin D synthesis pathways
- Prevents "shock" calculations for pale individuals suddenly exposed

## Scientific Basis

### UV-B and Vitamin D Synthesis
- Only UV-B wavelengths (290-315nm) produce vitamin D
- 7-dehydrocholesterol + UV-B → pre-vitamin D3 → vitamin D3
- Process self-regulates through photoisomerization equilibrium

### Altitude Effects
- UV increases ~10% per 1000m elevation
- Implemented as simple multiplier on base UV index

### Cloud Cover
- Already factored into UV index from weather API
- Clear sky UV only used for reference

### Daily Synthesis Limits
- Body naturally limits to ~20,000 IU/day
- Excess pre-vitamin D3 converts to inactive photoisomers
- Prevents toxicity from sun exposure alone

## Data Sources

1. **UV Index**: Open-Meteo API (includes cloud effects)
2. **Location**: iOS Core Location
3. **User Characteristics**: Apple Health (when available)
4. **Historical Data**: HealthKit vitamin D records

## Burn Time Calculation

Burn time is based on the **full MED** (Minimal Erythema Dose):

```
Burn Time = MED at UV 1 / Current UV
```

Real-world MED values at UV index 1:
- Type I: 150 minutes (burns in ~30 min at UV 5)
- Type II: 250 minutes (burns in ~45-50 min at UV 5)
- Type III: 425 minutes (burns in ~75-85 min at UV 5)
- Type IV: 600 minutes (burns in ~100-120 min at UV 5)
- Type V: 850 minutes (burns in ~150-180 min at UV 5)
- Type VI: 1100 minutes (rarely burns)

These values reflect actual outdoor conditions with natural cooling and movement.
The app notifies users at 80% of burn time as a safety warning.

## Vitamin D Winter

Above 35° latitude, UV-B is insufficient for vitamin D synthesis during winter months:
- **November-February**: Minimal to no synthesis
- **March & October**: Marginal synthesis (UV often < 3)
- App displays warning and recommends supplementation

## Safety Considerations

- Base rate calibrated to typical exposure patterns
- Burn time based on full MED
- Seasonal warnings for vitamin D winter
- Cannot reach toxic levels from UV exposure alone

## Future Improvements

1. **Spectral UV Data**: Use UV-B specific measurements when available
2. **Body Surface Area**: More precise calculation based on height/weight
3. **Seasonal Adjustments**: Winter UV-B availability at high latitudes
4. **Individual Calibration**: Learn from user's actual vitamin D blood tests

## Citations

- Holick, M.F. (2007). "Vitamin D deficiency." New England Journal of Medicine
- Webb, A.R. et al. (2018). "The role of sunlight exposure in determining the vitamin D status"
- Engelsen, O. (2010). "The relationship between ultraviolet radiation exposure and vitamin D status"
- MacLaughlin, J. & Holick, M.F. (1985). "Aging decreases the capacity of human skin to produce vitamin D3"
- Çekmez, Y. et al. (2024). "Time and duration of vitamin D synthesis" PMC10861575
- Various studies on MED and safe sun exposure from SunSmart Australia
