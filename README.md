앞으로 보게될 이 프로젝트야. 1990년대 퍼즐게임인 뿌요뿌요의 카피 게임이야. 지금까지 기초 구조는 gemini cli 를 이용해서 만들었다.  gemini 가 제안한 구조는 아래와 같아.  Thinking 은 최대한 짧게 해줘. 프로젝트 파일들은 열어보지 말고. 내가 보여주는 내용만 이해해 놓으면 돼. 

 <state_snapshot>
       <overall_goal>
           Develop a 1990s retro-style Puyo Puyo clone in Java using LibGDX, featuring standalone execution, AI opponents, P2P
   networking, and future server-side matchmaking.
       </overall_goal>

       <active_constraints>
           - Framework: LibGDX (OpenGL/GPU accelerated) instead of Java Swing/AWT.
           - Architecture: MVC (Model-View-Controller) for clean separation of logic and rendering.
           - Design Patterns: Screen/State (flow management), Command (input/networking), Observer/Event (decoupling logic and
   effects).
           - Display: FHD resolution, 60 FPS target.
           - Assets: Internal TTF fonts (e.g., Neodgm), Sprite Sheets (PNG/GIF), SFX (WAV), BGM (MID/Streaming).
       </active_constraints>

       <key_knowledge>
           - Framework Choice: LibGDX 1.12.1 selected for its performance (GPU) and portability (Android) vs. LWJGL or Swing.
           - AI Strategy: Heuristic-based Greedy AI using `BoardSimulator` and `HeuristicEvaluator` to score up to 24 possible
   placements.
           - Networking: TCP (Socket) required for frame-perfect synchronization of board states and garbage Puyo.
           - Plan Location:
   /data/data/com.termux/files/home/.gemini/tmp/puyo/86fd6dad-f2cc-4ca4-bc39-9374c9a43ae2/plans/step2-screens.md
       </key_knowledge>

       <artifact_trail>
           - `settings.gradle`: Defined `core` and `desktop` modules.
           - `build.gradle`: Configured LibGDX, AI extension, and LWJGL3 backend dependencies.
           - `core/src/main/java/com/puyo/game/PuyoGame.java`: Game entry class with screen management lifecycle.
           - `desktop/src/main/java/com/puyo/game/DesktopLauncher.java`: LWJGL3 application runner (480x800 window).
           - Directory Structure: Established `logic/ai/`, `input/`, `screens/`, `network/`, and `assets/` subfolders.
       </artifact_trail>

       <file_system_state>
           - CWD: `/data/data/com.termux/files/home/Desktop/java/puyo`
           - MODULES: `core` (logic), `desktop` (launcher)
           - ASSETS: `assets/images`, `assets/sounds`, `assets/fonts`
       </file_system_state>

       <recent_actions>
           - Initialized multi-module Gradle project.
           - Defined package structure supporting MVC and Command patterns.
           - Set up the main Game class and Desktop launcher.
           - Summarized technical requirements in `hist.txt`.
       </recent_actions>

       <task_state>
           1. [DONE] Step 1: Project Initialization and Environment Setup.
           2. [DONE] Step 2: Implement Screen Management and Transitions (Menu/Play). 
           3. [TODO] Step 3: Core Board Model and Falling Logic. <-- CURRENT FOCUS
           4. [TODO] Step 4: Input Command System and Rendering Integration.
       </task_state>
   </state_snapshot>