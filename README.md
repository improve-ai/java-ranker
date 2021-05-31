# Improve AI for Android

## Fast AI Decisions for Android


It's like an AI *if/then* statement. Quickly make decisions that maximize revenue, performance, user retention, or any other metric.

## Installation

Improve is available through [CocoaPods](http://cocoapods.org). To install, add the following line to your Podfile:

```ruby
pod "Improve"
```

### Hello World (for Cowboys)!

What is the best greeting?

```Java
IMPDecisionModel.load(modelUrl).chooseFrom(Arrays.asList("Hello World", "Howdy World", "Yo World")).given(Map.of("language", "cowboy")).get();
```

*greeting* should result in *Howdy World* assuming it performs best when *language* is *cowboy*.

### Numbers Too

What discount should we offer?

```Java
decisionModel.chooseFrom(Arrays.asList(0.1, 0.2, 0.3)).get();

```

### Booleans

Dynamically enable feature flags for best performance...

```
featureFlag = decisionModel.given(deviceAttributes).chooseFrom(Arrays.asList(true, false)).get()
```

### Complex Objects

```Java
themeVariants = Arrays.asList(Map.of("textColor", "#000000", "backgroundColor", "#ffffff"),
                Map.of("textColor", "#F0F0F0", "backgroundColor", "#aaaaaa"));
theme = themeModel.chooseFrom(themeVariants).get();
```

Improve learns to use the attributes of each key and value in a complex variant to make the optimal decision.

Variants can be any JSON encodeable data structure of arbitrary complexity, including nested dictionaries, arrays, strings, numbers, nulls, and booleans.

## Models

A *DecisionModel* contains the AI decision logic, analogous to a large number of *if/then* statements.

Models are thread-safe and a single model can be used for multiple decisions.

### Synchronous Model Loading

```Java
// Load model from https URLs
product = IMPDecisionModel.load(modelUrl).chooseFrom(Arrays.asList("clutch", "dress", "jacket")).get();

// Load model from assets
product = IMPDecisionModel.loadFromAsset(context, "filename").chooseFrom(Arrays.asList("clutch", "dress", "jacket")).get();
```

Models can be loaded from the assets or from https URLs.

### Asynchronous Model Loading

Asynchronous model loading allows decisions to be made at any point, even before the model is loaded.  If the model isn't yet loaded or fails to load, the first variant will be returned as the decision.

```Java
IMPDecisionTracker tracker = new IMPDecisionTracker(appContext, "trackUrl");
IMPDecisionModel model = new IMPDecisionModel("greetings");
model.setTracker(tracker);
model.loadAsync(modelUrl, new IMPDecisionModel.IMPDecisionModelLoadListener() {
    @Override
    public void onFinish(IMPDecisionModel model, Exception e) {
        if(e != null) {
            Log.d("Tag", "Error loading model: " + e.getLocalizedMessage());
        } else {
            // the model is ready to go
            model.chooseFrom(Arrays.asList("Hello World", "Howdy World", "Yo World")).get();
        }
    }
});

// It is very unlikely that the model will be loaded by the time this is called,
// so "Hello World" would be returned and tracked as the decision
greeting = model.chooseFrom(Arrays.asList("Hello World", "Howdy World", "Yo World")).get()
```

## Tracking & Training Models

The magic of Improve AI is it's learning process, whereby models continuously improve by training on past decisions. To accomplish this, decisions and events are tracked to your deployment of the Improve AI Gym.

### Tracking Decisions

Set a *DecisionTracker* on the *DecisionModel* to automatically track decisions and enable learning.  A single *DecisionTracker* instance can be shared by multiple models.

```Java
IMPDecisionTracker tracker = new IMPDecisionTracker(appContext, "trackerUrl"); // trackUrl is obtained from your Gym configuration
int fontSize = (Integer)IMPDecisionModel.load(modelUrl).track(tracker).chooseFrom(Arrays.asList(12, 16, 20)).get();
```

The decision is lazily evaluated and then automatically tracked as being causal upon calling *get()*.

For this reason, wait to call *get()* until the decision will actually be used.

### Tracking Events

Events are the mechanism by which decisions are rewarded or penalized.  In most cases these will mirror the normal analytics events that your app tracks and can be integrated with any event tracking singletons in your app.

```Java
tracker.trackEvent("Purchased", Map.of("product_id", 8, "value", 19.99));
```

Like most analytics packages, *track* takes an *event* name and an optional *properties* dictionary.  The only property with special significance is *value*, which indicates a reward value for decisions that lead to that event.  

If *value* is ommitted then the default reward value of an event is *0.001*.

By default, each decision is rewarded the total value of all events that occur within 48 hours of the decision.

Assuming a typical app where user retention and engagement are valuable, we recommend tracking all of your analytics events with the *DecisionTracker*.  You can customize the rewards assignment logic later in the Improve AI Gym.

## Scoring, Ranking, and Deferred Decisions

Downloading a model can take some hundreds of milliseconds, which can be too long if an immediate decision is needed upon app start.

To solve this, use *DecisionModel.score* and and *DecisionModel.rank* to effectively defer a decision so it is instantly available in the next app session.

For this example, background music needs to play immediately upon app start so the model will load asynchronously, but we will **not** wait for the model to load to make the decision. Instead the decision will will use previously persisted scores to rank the songs.

```Java
IMPDecisionModel songsModel = new IMPDecisionModel("songs");
songsModel.setTracker(new IMPDecisionTracker(appContext, "trackUrl"));

// load the model and update the scores for future song plays
songsModel.loadAsync(songsModelUrl, new IMPDecisionModel.IMPDecisionModelLoadListener() {
    @Override
    public void onFinish(IMPDecisionModel songsModel, Exception e) {
        if(e == null) {
            // the songs model has loaded, update the song scores
            updatedScores = songsModel.score(rankedSongs, songPreferences);

            // persist the score for each song
            database.update(songs, updatedScores);
        }
    }
});

songScores = database.loadScoresForSongs(songs);
rankedSongs = IMPDecisionModel.rank(songs, songScores);

// if the model isn't yet loaded, the top ranked song will be instantly chosen and the decision will then be tracked on get()
song = songsModel.given(songPreferences).chooseFrom(rankedSongs).get();
playSong(song);
```

**Caveats:**

1. If the *score* is being associated directly with the song object, then the score attribute should be filtered out before passing the song as a variant to *DecisionoModel.score()* or *chooseFrom()*. Do not include a previously calculated score as an attribute in the variants. Doing so could create a noisy feedback loop in the training process as the model will attempt to use these past scores to predict a new score.  Model training is quite robust so it would likely still work but the overall learning process would likely be somewhat slower.
2. The givens passed to *DecisionModel.score()* should be fairly similar to the givens used when the decision is tracked.  No decision tracking or learning takes place in *DecisionModel.score()* so a gross mismatch between the *DecisionModel.score()* and *DecisionModel.chooseFrom* givens could reduce performance.
3. Even though it would work to provide only *rankedSongs[0]* (the top ranking variant) to *chooseFrom*, still provide up to 50 other ranked variants. The model training performance is best when it has additional variants to compare the chosen variant against.

Persisted scores can also be used in conjunction with a loaded model in order to quickly make a decision using the most recent givens/context using just a subset of the top scoring variants.  This blended approach of offline scoring with online decisions allows very fast decisions with nearly unlimited variants.

## Privacy

It is strongly recommended to never include Personally Identifiable Information (PII) in variants or givens so that it is never tracked or persisted in your Improve Gym analytics records.

## An Ask

Thank you so much for enjoying my labor of love. Please only use it to create things that are good, true, and beautiful. - Justin

## License

Improve AI is copyright Mind Blown Apps, LLC. All rights reserved.  May not be used without a license.
