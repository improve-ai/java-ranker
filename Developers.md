# Publish
## 1. Publish to mavenLocal
// Run this command in the terminal
$./gradlew publishToMavenLocal

The artifacts are published into .m2 folder of the user's home directory.
```
// Unix like system
~/.m2

// Windows
C:\Users\<username>\.m2
```

## 2. Import the library published to mavenLocal
// Step 1
Add 'mavenLocal()' to the repositories of the project level gradle script.
```
buildscript {
    repositories {
        mavenLocal()
    }

allprojects {
    repositories {
        mavenLocal()
    }
}
```

// Step 2
Import it the same way as other libraries
```
dependencies {
    implementation 'ai.improve:improveai-android:7.0.0'
}
```

## 3. Publish to Github Packages
### 3.1 Creating a personal access token
Please check this page for details.
https://docs.github.com/en/packages/learn-github-packages/about-permissions-for-github-packages

### 3.2 Set the personal access token as Environment variables
```
export GITHUB_USERNAME=${your_github_id}
export GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 3.2 Run this command in the terminal
$gradle publish

