# Improve AI for Android

Improve AI provides quick on-device AI decisions that get smarter over time. It's like an AI *if/then* statement. Replace guesses in your app's configuration with AI decisions to increase your app's revenue, user retention, or any other metric automatically.

## Installation
### Step 1: Add JitPack in your root build.gradle at the end of repositories
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add the dependency in your app/build.gradle file
```gradle
dependencies {
    implementation 'com.github.improve-ai:android-sdk:7.1.3'
}
```

## Initialization
### Step 1: Add default track url to your AndroidManifest.xml file
```
// The default track url is obtained from your Improve AI Gym configuration.
<application>
    <meta-data
        android:name="ai.improve.DEFAULT_TRACK_URL"
        android:value="YOUR TRACK URL" />
</application>
```

### Step 2: Load the model
```
public class SampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // greetingsModelUrl is a trained model output by the Improve AI Gym
        DecisionModel.get("greetings").loadAsync(greetingsModelUrl);
    }
}
```

## Usage

Improve AI makes quick on-device AI decisions that get smarter over time.

The heart of Improve AI is the *which* statement. *which* is like an AI if/then statement.
```Java
greeting = DecisionModel.get("greetings").which("Hello", "Howdy", "Hola");
```

*which* makes decisions on-device using a *decision model*. Decision models are easily trained by assigning rewards for positive outcomes.

```Java
if (success) {
    DecisionModel.get("greetings").addReward(1.0);
}
```

Rewards are credited to the most recent decision made by the model. *which* will make the decision that provides the highest expected reward.  When the rewards are business metrics, such as revenue or user retention, the decisions will optimize to automatically improve those metrics over time.

*That's like A/B testing on steroids.*

### Numbers Too

What discount should we offer?

```Java
discount = decisionModel.which(0.1, 0.2, 0.3);
```

### Booleans

Dynamically enable feature flags for best performance...

```Java
featureFlag = decisionModel.given(deviceAttributes).which(true, false)
```

### Complex Objects

```Java
themeVariants = Arrays.asList(
    Map.of("textColor", "#000000", "backgroundColor", "#ffffff"),
    Map.of("textColor", "#F0F0F0", "backgroundColor", "#aaaaaa"));
theme = themeModel.which(themeVariants);
```

When a single Array argument is passed to which, it is treated as a list of variants.

Improve learns to use the attributes of each key and value in a complex variant to make the optimal decision.

Variants can be any JSON encodeable data structure of arbitrary complexity, including nested dictionaries, arrays, strings, numbers, nulls, and booleans.

## Decisions are Contextual

Unlike A/B testing or feature flags, Improve AI uses *context* to make the best decision for each user. On Android, the following context is automatically included:

- $country - two letter country code
- $lang - two letter language code
- $tz - numeric GMT offset
- $carrier - cellular network
- $device - string portion of device model
- $devicev - device version
- $os - string portion of OS name
- $osv - OS version
- $pixels - screen width x screen height
- $app - app name
- $appv - app version
- $sdkv - Improve AI SDK version
- $weekday - (ISO 8601, monday==1.0, sunday==7.0) plus fractional part of day
- $time - fractional day since midnight
- $runtime - fractional days since session start
- $day - fractional days since born
- $d - the number of decisions for this model
- $r - total rewards for this model
- $r/d - total rewards/decisions
- $d/day - decisions/$day

Using the context, on a Spanish speaker's device we expect our *greetings* model to learn to choose *Hola*.

Custom context can also be provided via *given()*:

```Java
greeting = greetingsModel.given(Map.of("language", "cowboy")).which("Hello", "Howdy", "Hola");
```

Given the language is *cowboy*, the variant with the highest expected reward should be *Howdy* and the model would learn to make that choice.

## Decision Models

## Example: Optimizing an Upsell Offer

Improve AI is powerful and flexible.  Variants can be any JSON encodeable data structure including **strings**, **numbers**, **booleans**, **lists**, and **maps**.

For a dungeon crawler game, say the user was purchasing an item using an In App Purchase.  We can use Improve AI to choose an additional product to display as an upsell offer during checkout. With a few lines of code, we can train a model that will learn to optimize the upsell offer given the original product being purchased.

```Java
product = Map.of("name", "red sword", "price", 4.99);

upsell = upsellModel.given(product).which(
          Map.of("name", "gold", "quantity", 100, "price", 1.99),
          Map.of("name", "diamonds", "quantity", 10, "price", 2.99),
          Map.of("name", "red scabbard", "price", 0.99);
```
The product to be purchased is the **red sword**.  Notice that the variants are maps with a mix of string and numeric values.

The rewards in this case might be any additional revenue from the upsell.

```Java
if (upsellPurchased) {
    upsellModel.addReward(upsell.price);
}
```

While it is reasonable to hypothesize that the **red scabbord** might be the best upsell offer to pair with the **red sword**, it is still a guess. Any time a guess is made on the value of a variable, instead use Improve AI to decide.

*Replace guesses with AI decisions.*

## Example: Performance Tuning

In the 2000s I was writing a lot of video streaming code. The initial motivation for Improve AI came out of my frustrations with attempting to tune video streaming clients across heterogenious networks.

I was forced to make guesses on performance sensitive configuration defaults through slow trial and error. My client configuration code maybe looked something like this:

```Java
config = Map.of("bufferSize", 2048, "videoBitrate", 384000);
```

This is the code I wish I could have written:

```Java
config = configModel.which(Map.of(
      "bufferSize", [1024, 2048, 4096, 8192],
      "videoBitrate", [256000, 384000, 512000]);
```
This example decides multiple variables simultaneously.  Notice that instead of a single list of variants, a dictionary mapping keys to lists of variants is provided to *which*. This multi-variate mode jointly optimizes both variables for the highest expected reward.  

The rewards in this case might be negative to penalize any stalls during video playback.
```Java
if (videoStalled) {
    configModel.addReward(-0.001);
}
```

Improve AI frees us from having to overthink our configuration values during development. We simply give it some reasonable variants and let it learn from real world usage.

Look for places where you're relying on guesses or an executive decision and consider instead directly optimizing for the outcomes you desire.

## Privacy

It is strongly recommended to never include Personally Identifiable Information (PII) in variants or givens so that it is never tracked, persisted, or used as training data.

## Help Improve Our World

The mission of Improve AI is to make our corner of the world a little bit better each day. When each of us improve our corner of the world, the whole world becomes better. If your product or work does not make the world better, do not use Improve AI. Otherwise, welcome, I hope you find value in my labor of love. - Justin Chapweske
