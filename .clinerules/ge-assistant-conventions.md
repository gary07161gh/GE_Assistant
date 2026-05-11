## Brief overview
Project-specific guidelines for the GE Assistant RuneLite plugin — a Java/Gradle project for active Grand Exchange flipping. These cover the coding conventions, design patterns, and architectural preferences observed throughout the codebase.

## Naming conventions
- Use PascalCase for class names: e.g., `GeAssistantPlugin`, `GeWarningEvaluator`, `PriceSnapshot`
- Use camelCase for method and field names: e.g., `refreshPricesAsync()`, `getReferencePrice()`
- Prefix GE-related classes with `Ge`: `GeWarning`, `GeOfferInsight`, `GeOfferInput`
- Suffix data-model classes with descriptive nouns: `Snapshot`, `Input`, `Insight`, `Warning`
- Suffix logic/evaluator classes with their role: `Evaluator`, `Builder`, `Client`, `Formatter`, `Locator`
- Package name: `com.geassistant` (no nested subpackages)
- Append `Async` to asynchronous method names: `refreshPricesAsync()`

## Design patterns
- **Interface-based polymorphism**: Model domain inputs via interfaces (e.g., `GeOfferInput`) with multiple concrete implementations (`OfferSnapshot`, `SetupOfferSnapshot`)
- **Builder pattern**: Use dedicated builder classes for constructing complex domain objects (e.g., `GeOfferInsightBuilder`)
- **Immutable data classes**: Expose fields via getters only, inject all dependencies through the constructor, avoid setters
- **Optional return types**: Return `Optional<T>` from methods that may not produce a result, using `Optional.empty()` and `Optional.of()` consistently
- **Unmodifiable view pattern**: Expose internal collections to external callers via `Collections.unmodifiableCollection()` / `Collections.unmodifiableMap()`
- **ConcurrentHashMap**: Store slot-indexed state in `ConcurrentHashMap<Integer, T>` for thread-safe read/write
- **ExecutorService + AtomicBoolean guard**: Use a single-thread executor with `AtomicBoolean.compareAndSet()` to prevent overlapping async refreshes
- **Daemon thread factory**: Create daemon threads for background tasks so they don't block JVM shutdown

## Code style
- Use Allman-style braces (opening brace on same line as declaration — standard Java style: `void method() {`)
- Indent with 4 spaces (no tabs)
- Declare internal/package-private classes with `final class` (no `public` modifier for internal types)
- Mark helper classes that are only used in one place as `private static final` inner classes
- Use SLF4J logging with context-rich messages: `log.info("GE Assistant saw slot {} item {} price {} qty {}", slot, itemId, price, qty)`
- Include `Locale.US` when calling `String.format()` for predictable number formatting
- Guard null/zero values early in methods (return `Optional.empty()` or early return)
- Use `@Override` when implementing interfaces or overriding methods
- Use Java 11 language features (`var` is avoided; explicit types preferred)

## RuneLite-specific conventions
- Annotate plugin class with `@PluginDescriptor(name = "...")`
- Inject dependencies via `@Inject` field injection (no constructor injection for plugin dependencies)
- Subscribe to game events with `@Subscribe` on public methods named `on[EventName]`
- Provide configuration via `@Provides` method returning the config interface
- Configuration interfaces extend `net.runelite.client.config.Config` with `@ConfigGroup("...")` and `@ConfigItem` annotations on default methods
- Override `startUp()` and `shutDown()` for plugin lifecycle, calling `overlayManager.add()`/`overlayManager.remove()`
- Use `client.getGrandExchangeOffers()` to read GE state

## Project metadata
- Java 11 (set via `options.release.set(11)` in Gradle)
- RuneLite version: `1.12.26.3`
- Build system: Gradle with `build.gradle`
- Main test runner class in build config (not main)
- JUnit 4 for tests
- Gson 2.11.0 for JSON deserialization
- Target Java source compatible via `compileOnly` for RuneLite client dependency

## Logging
- Use `LoggerFactory.getLogger(ClassName.class)` with `private static final` for logger instances
- Log lifecycle events (start/stop) at INFO level
- Log state changes (new offer detected, warning triggered, prices refreshed) at INFO level
- Log cleanup or minor state transitions at DEBUG level
- Log errors (API failures, rejections) at WARN level
- Include relevant identifiers (slot number, item ID, price, quantity) in every log message

## Testing conventions
- Test files located under `src/test/`
- Use JUnit 4 (`@Test` annotation)
- Test class naming: follow same class naming as production code under test