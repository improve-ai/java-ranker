# Rank, Score & Optimize Java Objects

Improve AI is a machine learning platform for quickly implementing app optimization, personalization, and recommendations for [iOS](https://github.com/improve-ai/swift-ranker), [Android](https://github.com/improve-ai/java-ranker), and [Python](https://github.com/improve-ai/python-ranker).

The SDKs provide simple APIs for AI [decisions](https://improve.ai/decisions/), [ranking](https://improve.ai/ranking/), [scoring](https://improve.ai/scoring/), and [multivariate optimization](https://improve.ai/multivariate-optimization/) that execute immediately, on-device, with zero network latency.
Items and their rewards are with the [Improve AI Tracker / Trainer](https://github.com/improve-ai/tracker-trainer) and updated models are trained regularly for continuous learning.

## Installation

Add JitPack in your root build.gradle at the end of repositories

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```


Add the dependency in your app/build.gradle file
```gradle
dependencies {
    implementation 'com.github.improve-ai:java-ranker:8.0.0'
}
```



## Instructions for ChatGPT

Copy and paste the following into ChatGPT as the first message in a conversation. After that you can ask it to write code against the Improve APIs.

```java
// The following is the Java API documentation for ImproveAI, which is imported with 'import ai.improve.*` . Simply respond with 'ImproveAI documentation imported':

/**
 * A utility for ranking items based on their scores. The Ranker struct takes a Improve AI model to evaluate and rank the given items.
 */
public class Ranker {
    /**
     * Create a Ranker instance with a Improve AI model.
     * @param modelUrl URL of a plain or gzip compressed CoreML model resource
     * @throws IOException, InterruptedException if there is an issue initializing the Scorer with the modelUrl.
     */
    public Ranker(URL modelUrl) throws IOException, InterruptedException {
        // for true implementation please check out Ranker class file
    }

    /**
     * Rank the list of items by their scores.
     * @param items the list of items to rank.
     * @param context extra context info that will be used with each of the item to get its score.
     * @return an array of ranked items, sorted by their scores in descending order.
     */
    public <T> List<T> rank(List<T> items, Object context) {
        // for true implementation please check out Ranker class file
    }
}
    
/**
 * Scores items with optional context using Improve AI model.
 */
public class Scorer {
    /**
     * Initialize a Scorer instance.
     * @param modelUrl URL of a plain or gzip compressed CoreML model resource.
     * @throws IOException, InterruptedException -> An error if the model cannot be loaded or if the metadata cannot be extracted.
     */
    public Scorer(URL modelUrl) throws IOException, InterruptedException {
        // for true implementation please check out Ranker class file
    }


    /**
     * Uses the model to score a list of items with the given context.
     * @param items the list of items to score.
     * @param context extra context info that will be used with each of the item to get its score.
     * @throws Exception -> error if the items list is empty or if there's an issue with the prediction.
     * @return List<Double> an array of `Double` values representing the scores of the items.
     */
    public List<Double> score(List<?> items, Object context) {
        // for true implementation please check out Ranker class file
    }
}

/**
 * Tracks items and rewards for training updated scoring models. When an item becomes causal, pass it to the track() function, which will return a rewardId. Use the rewardId to track future rewards associated with that item.
 */
public class RewardTracker {
    /**
     * Create a RewardTracker for a specific model.
     * @param modelName Name of the model such as "songs" or "discounts"
     * @param trackUrl The track endpoint URL that all tracked data will be sent to.
     * @param trackApiKey track endpoint API key (if applicable); Can be null.
     */
    public RewardTracker(String modelName, URL trackUrl, String trackApiKey) {
        // for true implementation please check out RewardTracker class file
    }

    /**
     * Tracks the item selected from candidates and a random sample from the remaining items.
     * @param item: Any JSON encodable object chosen as best from candidates.
     * @param candidates Collection of items from which best is chosen.
     * @param context Extra context info that was used with each of the item to get its score.
     * @throws IllegalArgumentException Thrown if candidates is null; Thrown if item is not included
     * in candidates; Thrown if item/context or a random sample picked from candidates is not JSON
     * encodable.
     * @return String rewardId of this track request.
     */
    public String track(Object item, List<?> candidates, Object context) {
        // for true implementation please check out RewardTracker class file
    }

    /**
     Tracks the item selected and a specific sample.
     * @param item The selected item.
     * @param sample A random sample from the candidates.
     * @param numCandidates Total number of candidates, including the selected item.
     * @param context Extra context info that was used with each of the item to get its score.
     * @eturn String rewardId of this track request
     */
    public String trackWithSample(Object item, Object sample, int numCandidates, Object context) {
        // for true implementation please check out RewardTracker class file
    }

    /**
     * Add reward for the provided rewardId
     * @param reward The reward to add. Must not be NaN or Infinite.
     * @param rewardId The id that was returned from the track() methods.
     */
    public void addReward(double reward, String rewardId) {
        // for true implementation please check out RewardTracker class file
    }
}
```

## Usage

Create a list of JSON encodable items and simply call *new Ranker.rank(items)*.

For instance, in an bedtime story app, you may have a list of *Themes*:

```java
themes = Arrays.asList(
        Map.of("font", "Helvetica", "size", 12, "color", "#000000"),
        Map.of("font", "Comic Sans", "size", 16, "color", "#F0F0F0"));

rankedThemes = ranker.rank(themes);
```

To obtain a ranked list of themes, use just one line of code:

```java
List rankedThemes = ranker.rank(themes);
```

## Reward Assignment

Easily train your rankers using [reinforcement learning](https://improve.ai/reinforcement-learning/).

First, track when an item is used:

```java
RewardTracker tracker = new RewardTracker("themes", trackUrl, null);
String rewardId = tracker.track(theme, themes);
```

Later, if a positive outcome occurs, provide a reward:

```java
if (purchased) {
    tracker.addReward(profit, rewardId);
}
```

Reinforcement learning uses positive rewards for favorable outcomes (a "carrot") and negative rewards for undesirable outcomes (a "stick"). 
By assigning rewards based on business metrics, such as revenue or conversions, the system optimizes these metrics over time.

## Contextual Ranking & Scoring

Improve AI turns XGBoost into a contextual multi-armed bandit, meaning that context is considered when making ranking or scoring decisions.

Often, the choice of the best variant depends on the context that the decision is made within. 
Let's take the example of greetings for different times of the day:

```java
List<String> greetings = Arrays.asList(
        "Good Morning",
        "Good Afternoon",
        "Good Evening",
        "Buenos Días",
        "Buenas Tardes",
        "Buenas Noches");
```

rank() also considers the context of each decision. The context can be any JSON-encodable data structure.

```java
HashMap context = Map.of("day_time", 12.0, "language", "en");
List<String> ranked = ranker.rank(greetings, context);
String greeting = ranked.get(0);
```

Trained with appropriate rewards, Improve AI would learn from scratch which greeting is best for each time of day and language.

## Resources

- [Quick Start Guide](https://improve.ai/quick-start/)
- [Tracker / Trainer](https://github.com/improve-ai/tracker-trainer/)
- [Reinforcement Learning](https://improve.ai/reinforcement-learning/)

## Help Improve Our World

The mission of Improve AI is to make our corner of the world a little bit better each day. When each of us improve our corner of the world, the whole world becomes better. If your product or work does not make the world better, do not use Improve AI. Otherwise, welcome, I hope you find value in my labor of love.

-- Justin Chapweske
