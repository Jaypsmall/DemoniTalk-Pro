import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const DemoniTalkApp());
}

class DemoniTalkApp extends StatefulWidget {
  const DemoniTalkApp({super.key});

  @override
  State<DemoniTalkApp> createState() => _DemoniTalkAppState();
}

class _DemoniTalkAppState extends State<DemoniTalkApp> {
  bool _isDarkMode = true;

  @override
  void initState() {
    super.initState();
    _loadTheme();
  }

  Future<void> _loadTheme() async {
    final settings = await NativeBridge.getSystemSettings();
    setState(() {
      _isDarkMode = settings['isDarkMode'] as bool? ?? true;
    });
  }

  void _toggleTheme(bool dark) {
    setState(() {
      _isDarkMode = dark;
    });
    NativeBridge.saveSystemSettings(isDarkMode: dark);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'DemoniTalk Pro',
      debugShowCheckedModeBanner: false,
      theme: _isDarkMode ? _darkTheme : _lightTheme,
      home: CommandScreen(
        isDarkMode: _isDarkMode,
        onThemeToggle: _toggleTheme,
      ),
    );
  }

  static const Color hellRed = Color(0xFFFF0600);
  static const Color brimstoneYellow = Color(0xFFFFD600);
  static const Color demoniPurple = Color(0xFF8A2BE2);
  static const Color obsidian = Color(0xFF121212);
  static const Color ashGrey = Color(0xFFB0BEC5);

  static final ThemeData _darkTheme = ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: obsidian,
    colorScheme: const ColorScheme.dark(
      primary: hellRed,
      secondary: brimstoneYellow,
      tertiary: demoniPurple,
      surface: Color(0xFF1E1E1E),
    ),
  );

  static final ThemeData _lightTheme = ThemeData(
    useMaterial3: true,
    brightness: Brightness.light,
    scaffoldBackgroundColor: const Color(0xFFF5F5F5),
    colorScheme: const ColorScheme.light(
      primary: hellRed,
      secondary: brimstoneYellow,
      tertiary: demoniPurple,
      surface: Colors.white,
    ),
  );
}

class NativeBridge {
  static const platform = MethodChannel('com.jaylizapp.demonitalk_pro/channel');

  static Future<bool> checkAndRequestAudioPermission() async {
    try {
      final bool result = await platform.invokeMethod('checkAndRequestAudioPermission');
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> hasAudioPermission() async {
    try {
      final bool result = await platform.invokeMethod('hasAudioPermission');
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> startFloatingService() async {
    try {
      final bool result = await platform.invokeMethod('startFloatingService');
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> stopFloatingService() async {
    try {
      final bool result = await platform.invokeMethod('stopFloatingService');
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> sendControlIntent(String action) async {
    try {
      final bool result = await platform.invokeMethod('sendControlIntent', {'action': action});
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> isAccessibilityEnabled() async {
    try {
      final bool result = await platform.invokeMethod('isAccessibilityEnabled');
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> openAccessibilitySettings() async {
    try {
      final bool result = await platform.invokeMethod('openAccessibilitySettings');
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> canDrawOverlays() async {
    try {
      final bool result = await platform.invokeMethod('canDrawOverlays');
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> openOverlaySettings() async {
    try {
      final bool result = await platform.invokeMethod('openOverlaySettings');
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> requestRoot() async {
    try {
      final bool result = await platform.invokeMethod('requestRoot');
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<List<Map<String, dynamic>>> getCommands() async {
    try {
      final List<dynamic> raw = await platform.invokeMethod('getCommands');
      return raw.map((e) => Map<String, dynamic>.from(e as Map)).toList();
    } catch (_) {
      return [];
    }
  }

  static Future<bool> saveCommands(List<Map<String, dynamic>> commands) async {
    try {
      final bool result = await platform.invokeMethod('saveCommands', {'commands': commands});
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<Map<String, dynamic>> getAiSettings() async {
    try {
      final Map raw = await platform.invokeMethod('getAiSettings');
      return Map<String, dynamic>.from(raw);
    } catch (_) {
      return {'aiEnabled': true, 'instructions': '', 'memory': ''};
    }
  }

  static Future<bool> saveAiSettings({
    required bool aiEnabled,
    required String instructions,
    required String memory,
  }) async {
    try {
      final bool result = await platform.invokeMethod('saveAiSettings', {
        'aiEnabled': aiEnabled,
        'instructions': instructions,
        'memory': memory,
      });
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<Map<String, dynamic>> getSystemSettings() async {
    try {
      final Map raw = await platform.invokeMethod('getSystemSettings');
      return Map<String, dynamic>.from(raw);
    } catch (_) {
      return {'executionMode': 'HYBRID', 'exportPath': '', 'isDarkMode': true};
    }
  }

  static Future<bool> saveSystemSettings({
    String? executionMode,
    String? exportPath,
    bool? isDarkMode,
  }) async {
    try {
      final current = await getSystemSettings();
      final bool result = await platform.invokeMethod('saveSystemSettings', {
        'executionMode': executionMode ?? current['executionMode'] ?? 'HYBRID',
        'exportPath': exportPath ?? current['exportPath'] ?? '',
        'isDarkMode': isDarkMode ?? current['isDarkMode'] ?? true,
      });
      return result;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> clearCache() async {
    try {
      final bool result = await platform.invokeMethod('clearCache');
      return result;
    } catch (_) {
      return false;
    }
  }
}

class CommandScreen extends StatefulWidget {
  final bool isDarkMode;
  final ValueChanged<bool> onThemeToggle;

  const CommandScreen({
    super.key,
    required this.isDarkMode,
    required this.onThemeToggle,
  });

  @override
  State<CommandScreen> createState() => _CommandScreenState();
}

class _CommandScreenState extends State<CommandScreen> {
  List<Map<String, dynamic>> _commands = [];
  bool _isEnglish = false;
  String _searchFilter = '';

  @override
  void initState() {
    super.initState();
    _reloadCommands();
    NativeBridge.checkAndRequestAudioPermission();
  }

  Future<void> _reloadCommands() async {
    final list = await NativeBridge.getCommands();
    setState(() {
      _commands = list;
    });
  }

  Future<void> _saveAndRefreshCommands(List<Map<String, dynamic>> newCommands) async {
    await NativeBridge.saveCommands(newCommands);
    setState(() {
      _commands = newCommands;
    });
  }

  void _showSnackBar(String text) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(text),
        duration: const Duration(seconds: 2),
        backgroundColor: _DemoniTalkAppState.hellRed,
      ),
    );
  }

  Widget _buildTitle() {
    return RichText(
      text: TextSpan(
        style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800),
        children: [
          const TextSpan(
            text: "De",
            style: TextStyle(color: _DemoniTalkAppState.hellRed),
          ),
          const TextSpan(
            text: "moni",
            style: TextStyle(color: _DemoniTalkAppState.brimstoneYellow),
          ),
          TextSpan(
            text: "Talk Pro 😈",
            style: TextStyle(
              color: widget.isDarkMode ? Colors.white : Colors.black87,
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final filteredCommands = _commands.where((cmd) {
      if (_searchFilter.isEmpty) return true;
      final t = (cmd['trigger'] ?? '').toString().toLowerCase();
      final a = (cmd['action'] ?? '').toString().toLowerCase();
      final query = _searchFilter.toLowerCase();
      return t.contains(query) || a.contains(query);
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: _buildTitle(),
        elevation: 0,
        backgroundColor: Theme.of(context).scaffoldBackgroundColor,
        actions: [
          IconButton(
            icon: Icon(widget.isDarkMode ? Icons.dark_mode : Icons.nights_stay),
            onPressed: () => widget.onThemeToggle(!widget.isDarkMode),
          ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(3.0),
          child: Container(
            height: 3,
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  Colors.transparent,
                  _DemoniTalkAppState.hellRed,
                  _DemoniTalkAppState.brimstoneYellow,
                  _DemoniTalkAppState.hellRed,
                  Colors.transparent,
                ],
              ),
            ),
          ),
        ),
      ),
      drawer: _buildDrawer(context),
      floatingActionButton: FloatingActionButton(
        backgroundColor: _DemoniTalkAppState.hellRed,
        foregroundColor: Colors.white,
        elevation: 8,
        onPressed: () => _openAddEditDialog(),
        child: const Icon(Icons.add, size: 28),
      ),
      body: Column(
        children: [
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14),
            child: Row(
              children: [
                Expanded(
                  child: _buildDemoniButton(
                    text: _isEnglish ? "(ACCESSIBILITY)" : "(ACCESIBILIDAD)",
                    color: _DemoniTalkAppState.demoniPurple,
                    onPressed: () async {
                      final enabled = await NativeBridge.isAccessibilityEnabled();
                      if (!enabled) {
                        _showSnackBar(_isEnglish
                            ? "Enable DemoniTalk in Accessibility"
                            : "Habilita DemoniTalk en Accesibilidad");
                        await NativeBridge.openAccessibilitySettings();
                      } else {
                        _showSnackBar(_isEnglish
                            ? "Accessibility already enabled 😈"
                            : "Accesibilidad ya activada 😈");
                      }
                    },
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 6),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14),
            child: Row(
              children: [
                Expanded(
                  child: _buildDemoniButton(
                    text: _isEnglish ? "START FLOATING" : "INICIAR BOTÓN",
                    color: Theme.of(context).colorScheme.primary,
                    onPressed: () async {
                      final canDraw = await NativeBridge.canDrawOverlays();
                      if (canDraw) {
                        await NativeBridge.startFloatingService();
                        _showSnackBar(_isEnglish
                            ? "Floating button active 😈"
                            : "Botón flotante activo 😈");
                      } else {
                        _showSnackBar(_isEnglish
                            ? "Grant overlay permission"
                            : "Concede el permiso para mostrar sobre otras apps");
                        await NativeBridge.openOverlaySettings();
                      }
                    },
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _buildDemoniButton(
                    text: _isEnglish ? "REQUEST ROOT" : "SOLICITAR ROOT",
                    color: Colors.redAccent.shade700,
                    onPressed: () async {
                      _showSnackBar(_isEnglish ? "Checking Root..." : "Verificando Root...");
                      final granted = await NativeBridge.requestRoot();
                      _showSnackBar(granted
                          ? (_isEnglish ? "Root Granted! 😈" : "¡Root Concedido! 😈")
                          : (_isEnglish ? "Root Denied" : "Root Denegado / No disponible"));
                    },
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Card(
            margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
              side: BorderSide(color: _DemoniTalkAppState.hellRed.withValues(alpha: 0.3)),
            ),
            color: widget.isDarkMode ? const Color(0xFF1E1E1E) : Colors.white,
            elevation: 4,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    _isEnglish ? "QUICK ACTIONS" : "ACCIONES RÁPIDAS",
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      color: Theme.of(context).colorScheme.primary,
                      letterSpacing: 1.0,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      _buildControlCircle(_DemoniTalkAppState.brimstoneYellow, () {
                        NativeBridge.sendControlIntent("ACTION_MODE_YELLOW");
                        _showSnackBar("Modo Amarillo Activado");
                      }),
                      _buildControlCircle(Colors.blue, () {
                        NativeBridge.sendControlIntent("ACTION_MODE_BLUE");
                        _showSnackBar("Modo Azul Activado (Vigilancia)");
                      }),
                      _buildControlCircle(Colors.green, () {
                        NativeBridge.sendControlIntent("ACTION_MODE_GREEN");
                        _showSnackBar("Modo Verde Activado (Escucha Continua)");
                      }),
                      _buildControlCircle(_DemoniTalkAppState.hellRed, () {
                        NativeBridge.sendControlIntent("ACTION_MODE_RED");
                        _showSnackBar("Detenido");
                      }),
                    ],
                  ),
                ],
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 8),
            child: TextField(
              decoration: InputDecoration(
                hintText: _isEnglish ? "Filter commands..." : "Buscar comando...",
                prefixIcon: const Icon(Icons.search, size: 20),
                isDense: true,
                contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              ),
              onChanged: (val) {
                setState(() {
                  _searchFilter = val;
                });
              },
            ),
          ),
          Expanded(
            child: filteredCommands.isEmpty
                ? Center(
                    child: Text(
                      _isEnglish ? "No commands found." : "No hay comandos registrados.",
                      style: const TextStyle(color: Colors.grey),
                    ),
                  )
                : ListView.builder(
                    padding: const EdgeInsets.only(bottom: 80, top: 4),
                    itemCount: filteredCommands.length,
                    itemBuilder: (context, index) {
                      final cmd = filteredCommands[index];
                      return _buildCommandCard(cmd);
                    },
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildControlCircle(Color color, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(22),
      child: Container(
        width: 44,
        height: 44,
        decoration: BoxDecoration(
          color: color,
          shape: BoxShape.circle,
          border: Border.all(color: Colors.white.withValues(alpha: 0.5), width: 2),
          boxShadow: [
            BoxShadow(
              color: color.withValues(alpha: 0.4),
              blurRadius: 8,
              offset: const Offset(0, 3),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDemoniButton({
    required String text,
    required Color color,
    required VoidCallback onPressed,
  }) {
    return ElevatedButton(
      style: ElevatedButton.styleFrom(
        backgroundColor: color,
        foregroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        padding: const EdgeInsets.symmetric(vertical: 14),
        elevation: 6,
      ),
      onPressed: onPressed,
      child: Text(
        text,
        style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 11),
      ),
    );
  }

  Widget _buildCommandCard(Map<String, dynamic> cmd) {
    final bool isRoot = cmd['isRoot'] as bool? ?? false;
    final String trigger = (cmd['trigger'] ?? '').toString();
    final String action = (cmd['action'] ?? '').toString();

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 5),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: isRoot
              ? _DemoniTalkAppState.brimstoneYellow.withValues(alpha: 0.4)
              : _DemoniTalkAppState.hellRed.withValues(alpha: 0.4),
        ),
      ),
      color: widget.isDarkMode ? const Color(0xFF1E1E1E) : Colors.white,
      elevation: 4,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () => _openAddEditDialog(commandToEdit: cmd),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      trigger.toUpperCase(),
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w900,
                        color: isRoot
                            ? _DemoniTalkAppState.brimstoneYellow
                            : Theme.of(context).colorScheme.primary,
                        letterSpacing: 1.0,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      action,
                      style: const TextStyle(fontSize: 12, color: Colors.grey),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              IconButton(
                icon: const Icon(Icons.delete, color: _DemoniTalkAppState.hellRed),
                onPressed: () {
                  final newCmds = List<Map<String, dynamic>>.from(_commands)..remove(cmd);
                  _saveAndRefreshCommands(newCmds);
                  _showSnackBar(_isEnglish ? "Command deleted" : "Comando eliminado");
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDrawer(BuildContext context) {
    return Drawer(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      child: ListView(
        padding: EdgeInsets.zero,
        children: [
          DrawerHeader(
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surface,
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _buildTitle(),
                const SizedBox(height: 8),
                Text(
                  "Version 2.0.2 Pro (Flutter)",
                  style: TextStyle(
                    fontSize: 12,
                    color: widget.isDarkMode ? _DemoniTalkAppState.ashGrey : Colors.grey.shade700,
                  ),
                ),
              ],
            ),
          ),
          _buildDrawerItem(
            icon: Icons.settings,
            text: _isEnglish ? "Settings" : "Ajustes",
            onTap: () {
              Navigator.pop(context);
              _openSettingsDialog();
            },
          ),
          _buildDrawerItem(
            icon: Icons.language,
            text: _isEnglish ? "Language: English" : "Idioma: Español",
            onTap: () {
              setState(() {
                _isEnglish = !_isEnglish;
              });
              Navigator.pop(context);
            },
          ),
          _buildDrawerItem(
            icon: Icons.storage,
            text: _isEnglish ? "Storage & Database" : "Almacenamiento de Comandos",
            onTap: () {
              Navigator.pop(context);
              _openStorageDialog();
            },
          ),
          _buildDrawerItem(
            icon: Icons.file_upload_outlined,
            text: _isEnglish ? "Import Backup (JSON)" : "Importar Respaldo (JSON)",
            onTap: () {
              Navigator.pop(context);
              _openImportDialog();
            },
          ),
          _buildDrawerItem(
            icon: Icons.file_download_outlined,
            text: _isEnglish ? "Export Backup (JSON)" : "Exportar Respaldo (JSON)",
            onTap: () {
              Navigator.pop(context);
              _openExportDialog();
            },
          ),
          _buildDrawerItem(
            icon: Icons.psychology,
            iconColor: _DemoniTalkAppState.demoniPurple,
            text: _isEnglish ? "AI Brain Config" : "Cerebro IA",
            onTap: () {
              Navigator.pop(context);
              _openAiConfigDialog();
            },
          ),
          _buildDrawerItem(
            icon: Icons.help_outline,
            iconColor: _DemoniTalkAppState.brimstoneYellow,
            text: _isEnglish ? "Help & Guide" : "Ayuda y Guía",
            onTap: () {
              Navigator.pop(context);
              _openHelpDialog();
            },
          ),
          const Divider(),
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Text(
              "Created by JAYLIZ with ❤️",
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 11,
                color: widget.isDarkMode ? _DemoniTalkAppState.ashGrey : Colors.grey,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDrawerItem({
    required IconData icon,
    required String text,
    Color? iconColor,
    required VoidCallback onTap,
  }) {
    return ListTile(
      leading: Icon(icon, color: iconColor ?? Theme.of(context).colorScheme.primary),
      title: Text(text, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
      onTap: onTap,
    );
  }

  void _openAddEditDialog({Map<String, dynamic>? commandToEdit}) {
    final isEdit = commandToEdit != null;
    final triggerController = TextEditingController(text: commandToEdit?['trigger'] ?? '');
    final actionController = TextEditingController(text: commandToEdit?['action'] ?? '');
    bool isRoot = commandToEdit?['isRoot'] as bool? ?? false;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text(
            isEdit ? "EDITAR COMANDO" : "NUEVO COMANDO",
            style: const TextStyle(
              color: _DemoniTalkAppState.hellRed,
              fontWeight: FontWeight.w900,
            ),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: triggerController,
                decoration: const InputDecoration(
                  labelText: "Palabra Clave",
                  hintText: "ej. cámara, primer chat, reproduce...",
                ),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: actionController,
                decoration: const InputDecoration(
                  labelText: "Acción / Comando",
                  hintText: "ej. com.android.camera, type:hola...",
                ),
              ),
              const SizedBox(height: 8),
              CheckboxListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text("Requiere Root 😈", style: TextStyle(fontSize: 13)),
                value: isRoot,
                onChanged: (val) {
                  setDialogState(() {
                    isRoot = val ?? false;
                  });
                },
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text("CANCELAR", style: TextStyle(color: Colors.grey)),
            ),
            TextButton(
              onPressed: () {
                final t = triggerController.text.trim();
                final a = actionController.text.trim();
                if (t.isEmpty || a.isEmpty) return;

                final newCmd = {'trigger': t, 'action': a, 'isRoot': isRoot};
                final newCmds = List<Map<String, dynamic>>.from(_commands);

                if (isEdit) {
                  final idx = newCmds.indexOf(commandToEdit);
                  if (idx != -1) newCmds[idx] = newCmd;
                } else {
                  newCmds.add(newCmd);
                }

                _saveAndRefreshCommands(newCmds);
                Navigator.pop(ctx);
                _showSnackBar(isEdit ? "Comando actualizado" : "Comando guardado 😈");
              },
              child: const Text(
                "GUARDAR",
                style: TextStyle(color: _DemoniTalkAppState.hellRed, fontWeight: FontWeight.bold),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _openSettingsDialog() async {
    final system = await NativeBridge.getSystemSettings();
    if (!mounted) return;
    String currentMode = system['executionMode'] ?? 'HYBRID';
    final pathController = TextEditingController(text: system['exportPath'] ?? '');

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text(
            "AJUSTES DEL SISTEMA",
            style: TextStyle(color: _DemoniTalkAppState.hellRed, fontWeight: FontWeight.w900),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text("RUTA DE EXPORTACIÓN", style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              TextField(
                controller: pathController,
                decoration: const InputDecoration(
                  hintText: "/sdcard/Download",
                  isDense: true,
                ),
              ),
              const SizedBox(height: 16),
              const Text("MODO DE EJECUCIÓN DE COMANDOS:", style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold)),
              RadioListTile<String>(
                dense: true,
                title: const Text("Modo Demoníaco (Root + Accesibilidad)", style: TextStyle(fontSize: 12)),
                value: "HYBRID",
                groupValue: currentMode,
                onChanged: (val) => setDialogState(() => currentMode = val!),
              ),
              RadioListTile<String>(
                dense: true,
                title: const Text("Modo Root", style: TextStyle(fontSize: 12)),
                value: "ROOT",
                groupValue: currentMode,
                onChanged: (val) => setDialogState(() => currentMode = val!),
              ),
              RadioListTile<String>(
                dense: true,
                title: const Text("Modo Accesibilidad", style: TextStyle(fontSize: 12)),
                value: "ACCESSIBILITY",
                groupValue: currentMode,
                onChanged: (val) => setDialogState(() => currentMode = val!),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text("CANCELAR"),
            ),
            TextButton(
              onPressed: () async {
                await NativeBridge.saveSystemSettings(
                  executionMode: currentMode,
                  exportPath: pathController.text.trim(),
                );
                if (!ctx.mounted) return;
                Navigator.pop(ctx);
                _showSnackBar("Ajustes guardados correctamente 😈");
              },
              child: const Text("GUARDAR", style: TextStyle(color: _DemoniTalkAppState.hellRed, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  void _openStorageDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("ALMACENAMIENTO DE COMANDOS", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("Total de entradas guardadas: ${_commands.length}", style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            Container(
              height: 180,
              width: double.maxFinite,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey.withValues(alpha: 0.3)),
                borderRadius: BorderRadius.circular(8),
              ),
              child: ListView.builder(
                itemCount: _commands.length,
                itemBuilder: (context, i) {
                  final cmd = _commands[i];
                  return ListTile(
                    dense: true,
                    title: Text(cmd['trigger'] ?? '', style: const TextStyle(fontWeight: FontWeight.bold)),
                    subtitle: Text(cmd['action'] ?? '', maxLines: 1),
                  );
                },
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () async {
              await NativeBridge.clearCache();
              await _reloadCommands();
              if (!ctx.mounted) return;
              Navigator.pop(ctx);
              _showSnackBar("¡Base de datos restablecida!");
            },
            child: const Text("RESTABLECER", style: TextStyle(color: Colors.red)),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text("CERRAR"),
          ),
        ],
      ),
    );
  }

  void _openAiConfigDialog() async {
    final ai = await NativeBridge.getAiSettings();
    if (!mounted) return;
    bool aiEnabled = ai['aiEnabled'] ?? true;
    final instructionsController = TextEditingController(text: ai['instructions'] ?? '');
    final memoryController = TextEditingController(text: ai['memory'] ?? '');

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text(
            "CEREBRO Y MEMORIA IA",
            style: TextStyle(color: _DemoniTalkAppState.demoniPurple, fontWeight: FontWeight.w900),
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text("Respuestas de IA", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                  value: aiEnabled,
                  onChanged: (val) => setDialogState(() => aiEnabled = val),
                ),
                const SizedBox(height: 8),
                const Text("MOTOR DE IA ACTIVO: IA Gratuita pública (Sin API key).", style: TextStyle(fontSize: 11, color: Colors.grey)),
                const SizedBox(height: 12),
                TextField(
                  controller: instructionsController,
                  maxLines: 2,
                  decoration: const InputDecoration(
                    labelText: "Pautas / Instrucciones",
                    hintText: "ej. Háblame de tú, llámame Jefe...",
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: memoryController,
                  maxLines: 3,
                  decoration: const InputDecoration(
                    labelText: "Memoria guardada",
                    hintText: "ej. • Mi cumpleaños es el 15 de marzo...",
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text("CANCELAR"),
            ),
            TextButton(
              onPressed: () async {
                await NativeBridge.saveAiSettings(
                  aiEnabled: aiEnabled,
                  instructions: instructionsController.text.trim(),
                  memory: memoryController.text.trim(),
                );
                if (!ctx.mounted) return;
                Navigator.pop(ctx);
                _showSnackBar("Memoria IA guardada 😈");
              },
              child: const Text("GUARDAR", style: TextStyle(color: _DemoniTalkAppState.demoniPurple, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  void _openHelpDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("GUÍA Y FUNCIONAMIENTO", style: TextStyle(color: _DemoniTalkAppState.hellRed, fontWeight: FontWeight.w900)),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: const [
              Text("FUNCIONAMIENTO GENERAL", style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: _DemoniTalkAppState.hellRed)),
              SizedBox(height: 4),
              Text("DemoniTalk Pro te permite controlar tu Android mediante voz y acciones flotantes. Di 'Demoni' o 'Demonio' seguido de tu orden.", style: TextStyle(fontSize: 12)),
              SizedBox(height: 12),
              Text("MODOS DE ACTIVACIÓN:", style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: _DemoniTalkAppState.brimstoneYellow)),
              SizedBox(height: 4),
              Text("• Amarillo: Escucha única\n• Azul: Modo Vigilancia (Wake word)\n• Verde: Escucha Continua\n• Rojo: Detener todo", style: TextStyle(fontSize: 12)),
              SizedBox(height: 12),
              Text("MODOS DE EJECUCIÓN:", style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: _DemoniTalkAppState.demoniPurple)),
              SizedBox(height: 4),
              Text("• Modo Demoníaco: Intenta Root y cae suavemente a Accesibilidad.\n• Modo Root: Exclusivamente comandos Root Shell.\n• Modo Accesibilidad: Interacción táctil en pantalla sin Root.", style: TextStyle(fontSize: 12)),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text("ENTENDIDO", style: TextStyle(color: _DemoniTalkAppState.hellRed, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  void _openImportDialog() {
    final importController = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(
          _isEnglish ? "IMPORT COMMANDS (JSON)" : "IMPORTAR PACTOS / COMANDOS",
          style: const TextStyle(color: _DemoniTalkAppState.hellRed, fontWeight: FontWeight.w900, fontSize: 16),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              _isEnglish
                  ? "Paste your JSON backup code below or from the clipboard:"
                  : "Pega el código JSON de tu respaldo o de tu otra app a continuación:",
              style: const TextStyle(fontSize: 12),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: importController,
              maxLines: 6,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 11),
              decoration: const InputDecoration(
                hintText: '[\n  {\n    "trigger": "cámara",\n    "action": "com.android.camera",\n    "isRoot": false\n  }\n]',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () async {
              final data = await Clipboard.getData(Clipboard.kTextPlain);
              if (data?.text != null) {
                importController.text = data!.text!;
              }
            },
            child: Text(_isEnglish ? "PASTE CLIPBOARD" : "PEGAR DE PORTAPAPELES"),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(_isEnglish ? "CANCEL" : "CANCELAR", style: const TextStyle(color: Colors.grey)),
          ),
          TextButton(
            onPressed: () {
              final text = importController.text.trim();
              if (text.isEmpty) return;

              try {
                final List parsed = jsonDecode(text) as List;
                final newCmds = parsed.map((e) {
                  final map = e as Map<String, dynamic>;
                  return {
                    'trigger': map['trigger']?.toString() ?? '',
                    'action': map['action']?.toString() ?? '',
                    'isRoot': map['isRoot'] == true,
                  };
                }).where((c) => (c['trigger'] as String).isNotEmpty).toList();

                _saveAndRefreshCommands(newCmds);
                Navigator.pop(ctx);
                _showSnackBar(_isEnglish
                    ? "${newCmds.length} commands restored successfully! 😈"
                    : "¡${newCmds.length} comandos importados con éxito! 😈");
              } catch (e) {
                _showSnackBar(_isEnglish ? "Invalid JSON format: $e" : "Formato JSON no válido: $e");
              }
            },
            child: Text(
              _isEnglish ? "IMPORT" : "IMPORTAR",
              style: const TextStyle(color: _DemoniTalkAppState.hellRed, fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
    );
  }

  void _openExportDialog() {
    final jsonText = const JsonEncoder.withIndent('  ').convert(_commands);

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(
          _isEnglish ? "EXPORT COMMANDS (JSON)" : "EXPORTAR RESPALDO JSON",
          style: const TextStyle(color: _DemoniTalkAppState.hellRed, fontWeight: FontWeight.w900, fontSize: 16),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              _isEnglish
                  ? "Copy this JSON structure to restore your commands anywhere:"
                  : "Copia este código JSON para guardar o transferir tus tarjetas:",
              style: const TextStyle(fontSize: 12),
            ),
            const SizedBox(height: 8),
            Container(
              height: 180,
              width: double.maxFinite,
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: widget.isDarkMode ? Colors.black38 : Colors.grey.shade200,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.grey.withValues(alpha: 0.3)),
              ),
              child: SingleChildScrollView(
                child: SelectableText(
                  jsonText,
                  style: const TextStyle(fontFamily: 'monospace', fontSize: 11),
                ),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () {
              Clipboard.setData(ClipboardData(text: jsonText));
              Navigator.pop(ctx);
              _showSnackBar(_isEnglish ? "Copied to clipboard! 😈" : "¡Copiado al portapapeles! 😈");
            },
            child: Text(
              _isEnglish ? "COPY TO CLIPBOARD" : "COPIAR AL PORTAPAPELES",
              style: const TextStyle(color: _DemoniTalkAppState.hellRed, fontWeight: FontWeight.bold),
            ),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(_isEnglish ? "CLOSE" : "CERRAR"),
          ),
        ],
      ),
    );
  }
}
