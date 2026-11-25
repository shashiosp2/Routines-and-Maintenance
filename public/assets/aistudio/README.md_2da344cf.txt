# Obsidian Horizon

**Obsidian Horizon** is a unified toolkit for personal organization, combining recurring maintenance task tracking and interactive routine management into a single Obsidian plugin.

## Features

### 📅 Maintenance Tasks
- **Recurring Chores**: Track chores with custom frequencies (e.g., vacuum every 7 days).
- **Due Date Visualization**: Color-coded rows (Red for overdue, Orange/Yellow for upcoming) to help you prioritize.
- **History Logging**: Automatically logs task completions to a separate history file.
- **Custom Views**: Dedicated table view for managing and adding new tasks.

### ⏱ Routine Tracker
- **Timed Routines**: Follow step-by-step routines with interactive timers.
- **Dynamic Parsing**: Routines are parsed directly from your markdown tables.
- **Overtime Tracking**: See exactly how much extra time you're spending on a task.
- **Code Block Support**: Embed routine players directly into your notes using ` ```routines ` code blocks.

## Getting Started

### Installation
1.  Copy `main.js`, `manifest.json`, and `styles.css` (if applicable) to your Obsidian vault's `.obsidian/plugins/obsidian-horizon` folder.
2.  Enable the plugin in Obsidian's **Community Plugins** settings.

### Configuration
Go to **Settings > Obsidian Horizon** to configure:
- **Maintenance Task File**: Path to the markdown file for chores (e.g., `notes/maintenance.md`).
- **Maintenance History File**: Path where completed tasks are logged.
- **Routines Markdown File**: Path to the file defining your routines (e.g., `notes/routines.md`).

## Usage

### Maintenance Tasks
Use the command **Open Maintenance Tasks** or the ribbon icon to view your chores. To add a task, use the input row at the bottom of the table.

### Routine Tracker
Use the command **Open Routine Tracker** or the ribbon icon to start a session. You can also embed a routine player in any note by creating a `routines` code block.

---
*Created by Gemini CLI and the Obsidian Horizon team.*
