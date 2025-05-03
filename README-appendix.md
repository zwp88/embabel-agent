# Embabel Agent Goal Planning System

## Key Components and Their Relationships

### Action
- Core executable unit that performs a specific task
- Implements `GoapAction` interface and the `ActionRunner` interface
- Has preconditions (what must be true before it can run)
- Has effects (what becomes true after it runs)
- Contains inputs and outputs defined as `IoBinding` objects
- Can consume tools via the `ToolConsumer` interface
- Has a cost (computational expense) and value (benefit of execution)
- References transitions that define how action affects the system state

### Condition
- Represents a named predicate that can be evaluated
- Can be TRUE, FALSE, or UNKNOWN (via `ConditionDetermination`)
- Has a cost associated with evaluation
- Can be combined using logical operators (AND, OR, NOT)
- Used to define preconditions for Actions and Goals
- Evaluated against the current world state via `ProcessContext`

### Blackboard
- Shared memory system for maintaining context
- Implements `Bindable` interface for variable binding
- Stores objects in an ordered list accessible by name or type
- Can spawn independent child blackboards
- Can track condition values explicitly
- Acts as the repository for all data and objects in the system
- Used by the World State Determiner to derive current conditions

### WorldState
- Represents current state of the system as a map of conditions
- Key component in GOAP (Goal-Oriented Action Planning)
- Used to determine which actions are achievable
- Can generate variants with different condition values
- Foundation for planning algorithms

### Plan
- Sequence of Actions to achieve a Goal
- Has associated cost and value metrics
- Created by Planners (like `AStarGoapPlanner`)
- Represents the path from current state to goal state

### GoapPlanningSystem
- Collection of available Actions and Goals
- Tracks known preconditions and effects
- Used by planners to construct valid plans

## How They Work Together

### 1. Planning Process
- The `WorldStateDeterminer` reads the `Blackboard` to determine current conditions
- The `AStarGoapPlanner` uses A* algorithm to find optimal sequence of Actions
- Each Action's preconditions are checked against `WorldState`
- The planner constructs a `Plan` with lowest cost path to satisfy Goal conditions

### 2. Execution Flow
- Actions read inputs from and write outputs to the `Blackboard`
- Actions can only execute when their preconditions are satisfied
- When executed, Actions update the `WorldState` through their effects
- The system re-evaluates Conditions after each Action

### 3. State Management
- `Blackboard` maintains the actual objects and data
- `WorldState` maintains boolean conditions derived from `Blackboard`
- Conditions provide the evaluation logic to determine truth values
- Plans are re-assessed after each Action executes

This architecture implements Goal-Oriented Action Planning (GOAP), allowing the system to dynamically determine sequences of actions to achieve goals based on current conditions.

## Visualization

To review key components visually:
- Open Graphviz online: https://dreampuf.github.io/GraphvizOnline
- Paste the content of `embabel_planning_system.dot` in the left pane