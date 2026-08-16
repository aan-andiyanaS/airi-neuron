# Graph Report - .  (2026-08-16)

## Corpus Check
- Corpus is ~18,157 words - fits in a single context window. You may not need a graph.

## Summary
- 179 nodes · 217 edges · 20 communities (15 shown, 5 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 17 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- LlamaCpp JNI Native Layer
- Data Repository & Entities
- Chat UI Components & ViewModel
- Inference Manager & Tests
- UI Navigation & Core Screens
- Room Database & DAOs
- Image Processing Utilities
- Image Processing Tests
- JNI Bridge Interfaces
- Input Validator Utilities
- Output Filter Tests
- Performance Monitor Utilities
- Input Validator Tests
- Gradle Wrapper Scripts
- Output Filter Utilities

## God Nodes (most connected - your core abstractions)
1. `ChatViewModelTest` - 17 edges
2. `ChatViewModel` - 16 edges
3. `ChatRepositoryTest` - 14 edges
4. `ChatEntity` - 13 edges
5. `Java_com_airi_odslm_jni_LlamaCppBridge_infer()` - 11 edges
6. `ChatRepository` - 9 edges
7. `NativeContext` - 8 edges
8. `MainApp()` - 7 edges
9. `ImageProcessorTest` - 7 edges
10. `tokenize()` - 6 edges

## Surprising Connections (you probably didn't know these)
- `MainApp()` --calls--> `CharacterScreen()`  [INFERRED]
  app/src/main/kotlin/com/airi/odslm/ui/screens/MainApp.kt → app/src/main/kotlin/com/airi/odslm/ui/screens/CharacterScreen.kt
- `MainApp()` --calls--> `ChatScreen()`  [INFERRED]
  app/src/main/kotlin/com/airi/odslm/ui/screens/MainApp.kt → app/src/main/kotlin/com/airi/odslm/ui/screens/ChatScreen.kt
- `MainApp()` --calls--> `LibraryScreen()`  [INFERRED]
  app/src/main/kotlin/com/airi/odslm/ui/screens/MainApp.kt → app/src/main/kotlin/com/airi/odslm/ui/screens/LibraryScreen.kt
- `ChatRepositoryTest` --references--> `AppDatabase`  [EXTRACTED]
  app/src/androidTest/kotlin/com/airi/odslm/ChatRepositoryTest.kt → app/src/main/kotlin/com/airi/odslm/data/AppDatabase.kt
- `ChatRepositoryTest` --references--> `ChatDao`  [EXTRACTED]
  app/src/androidTest/kotlin/com/airi/odslm/ChatRepositoryTest.kt → app/src/main/kotlin/com/airi/odslm/data/ChatDao.kt

## Import Cycles
- None detected.

## Communities (20 total, 5 thin omitted)

### Community 0 - "LlamaCpp JNI Native Layer"
Cohesion: 0.14
Nodes (27): decodeBatch(), Java_com_airi_odslm_jni_LlamaCppBridge_infer(), Java_com_airi_odslm_jni_LlamaCppBridge_loadModel(), Java_com_airi_odslm_jni_LlamaCppBridge_unloadModel(), JNI_OnLoad(), JNI_OnUnload(), NativeContext, ctx (+19 more)

### Community 1 - "Data Repository & Entities"
Cohesion: 0.13
Nodes (6): ChatRepositoryTest, ChatEntity, MessageRole, ChatRepository, Flow, Result

### Community 2 - "Chat UI Components & ViewModel"
Cohesion: 0.10
Nodes (11): ChatBubble(), ChatScreen(), ChatMessage, ChatUiState, ChatViewModel, Factory, Uri, StateFlow (+3 more)

### Community 3 - "Inference Manager & Tests"
Cohesion: 0.10
Nodes (3): InferenceManager, Uri, ChatViewModelTest

### Community 4 - "UI Navigation & Core Screens"
Cohesion: 0.11
Nodes (12): ChatActivity, AppDestination, CHARACTER, CHAT, LIBRARY, BottomNavBar(), CharacterScreen(), LibraryScreen() (+4 more)

### Community 5 - "Room Database & DAOs"
Cohesion: 0.21
Nodes (7): AppDatabase, buildDatabase(), getInstance(), Context, ChatDao, Flow, RoomDatabase

### Community 6 - "Image Processing Utilities"
Cohesion: 0.29
Nodes (5): ImageProcessor, ByteArray, Context, Uri, Bitmap

### Community 7 - "Image Processing Tests"
Cohesion: 0.25
Nodes (4): ImageProcessorTest, Context, Uri, ContentResolver

### Community 9 - "Input Validator Utilities"
Cohesion: 0.33
Nodes (3): InputValidator, Context, Uri

### Community 13 - "Gradle Wrapper Scripts"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **7 isolated node(s):** `model`, `ctx`, `mctx`, `MessageRole`, `CHAT` (+2 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ChatViewModel` connect `Community 2` to `Community 1`, `Community 3`, `Community 4`?**
  _High betweenness centrality (0.154) - this node is a cross-community bridge._
- **Why does `ChatViewModelTest` connect `Community 3` to `Community 1`, `Community 2`?**
  _High betweenness centrality (0.108) - this node is a cross-community bridge._
- **Why does `ChatRepository` connect `Community 1` to `Community 2`, `Community 3`?**
  _High betweenness centrality (0.099) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `ChatEntity` (e.g. with `.`clearHistory removes all messages`()` and `.`messages are ordered by timestamp ascending`()`) actually correct?**
  _`ChatEntity` has 8 INFERRED edges - model-reasoned connections that need verification._
- **What connects `model`, `ctx`, `mctx` to the rest of the system?**
  _7 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.13756613756613756 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.1341991341991342 - nodes in this community are weakly interconnected._