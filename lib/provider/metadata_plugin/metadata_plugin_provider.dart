import 'dart:convert';
import 'dart:io';

import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:drift/drift.dart';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:path/path.dart';
import 'package:path_provider/path_provider.dart';
import 'package:spotube/models/database/database.dart';
import 'package:spotube/models/metadata/metadata.dart';
import 'package:spotube/provider/database/database.dart';
import 'package:spotube/provider/youtube_engine/youtube_engine.dart';
import 'package:spotube/services/dio/dio.dart';
import 'package:spotube/services/logger/logger.dart';
import 'package:spotube/services/metadata/errors/exceptions.dart';
import 'package:spotube/services/metadata/metadata.dart';
import 'package:spotube/utils/service_utils.dart';
import 'package:archive/archive.dart';
import 'package:pub_semver/pub_semver.dart';

final allowedDomainsRegex = RegExp(
  r"^(https?:\/\/)?(www\.)?(github\.com|codeberg\.org)\/.+",
);

class MetadataPluginState {
  final List<PluginConfiguration> plugins;
  final int defaultMetadataPlugin;
  final int defaultAudioSourcePlugin;

  const MetadataPluginState({
    this.plugins = const [],
    this.defaultMetadataPlugin = -1,
    this.defaultAudioSourcePlugin = -1,
  });

  PluginConfiguration? get defaultMetadataPluginConfig {
    if (defaultMetadataPlugin < 0 || defaultMetadataPlugin >= plugins.length) {
      return null;
    }
    return plugins[defaultMetadataPlugin];
  }

  PluginConfiguration? get defaultAudioSourcePluginConfig {
    if (defaultAudioSourcePlugin < 0 ||
        defaultAudioSourcePlugin >= plugins.length) {
      return null;
    }
    return plugins[defaultAudioSourcePlugin];
  }

  factory MetadataPluginState.fromJson(Map<String, dynamic> json) {
    return MetadataPluginState(
      plugins: (json["plugins"] as List<dynamic>)
          .map((e) => PluginConfiguration.fromJson(e))
          .toList(),
      defaultMetadataPlugin: json["default_metadata_plugin"] ?? -1,
      defaultAudioSourcePlugin: json['default_audio_source_plugin'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "plugins": plugins.map((e) => e.toJson()).toList(),
      "default_metadata_plugin": defaultMetadataPlugin,
      "default_audio_source_plugin": defaultAudioSourcePlugin
    };
  }

  MetadataPluginState copyWith({
    List<PluginConfiguration>? plugins,
    int? defaultMetadataPlugin,
    int? defaultAudioSourcePlugin,
  }) {
    return MetadataPluginState(
      plugins: plugins ?? this.plugins,
      defaultMetadataPlugin:
          defaultMetadataPlugin ?? this.defaultMetadataPlugin,
      defaultAudioSourcePlugin:
          defaultAudioSourcePlugin ?? this.defaultAudioSourcePlugin,
    );
  }
}

class MetadataPluginNotifier extends AsyncNotifier<MetadataPluginState> {
  AppDatabase get database => ref.read(databaseProvider);

  @override
  build() async {
    final database = ref.watch(databaseProvider);

    final subscription = database.pluginsTable.select().watch().listen(
      (event) async {
        state = AsyncValue.data(await toStatePlugins(event));
      },
    );

    ref.onDispose(() {
      subscription.cancel();
    });

    final plugins = await database.pluginsTable.select().get();

    final pluginState = await toStatePlugins(plugins);

    await _loadDefaultPlugins(pluginState);

    return pluginState;
  }

  Future<MetadataPluginState> toStatePlugins(
    List<PluginsTableData> plugins,
  ) async {
    int defaultMetadataPlugin = -1;
    int defaultAudioSourcePlugin = -1;
    final pluginConfigs = <PluginConfiguration>[];

    for (int i = 0; i < plugins.length; i++) {
      final plugin = plugins[i];

      final pluginConfig = PluginConfiguration(
        name: plugin.name,
        author: plugin.author,
        description: plugin.description,
        version: plugin.version,
        entryPoint: plugin.entryPoint,
        pluginApiVersion: plugin.pluginApiVersion,
        repository: plugin.repository,
        apis: plugin.apis
            .map(
              (e) => PluginApis.values.firstWhereOrNull(
                (api) => api.name == e,
              ),
            )
            .nonNulls
            .toList(),
        abilities: plugin.abilities
            .map(
              (e) => PluginAbilities.values.firstWhereOrNull(
                (ability) => ability.name == e,
              ),
            )
            .nonNulls
            .toList(),
      );

      final pluginExtractionDir = await _getPluginExtractionDir(pluginConfig);
      final pluginJsonFile =
          File(join(pluginExtractionDir.path, "plugin.json"));
      final pluginBinaryFile =
          File(join(pluginExtractionDir.path, "plugin.out"));

      if (!await pluginExtractionDir.exists() ||
          !await pluginJsonFile.exists() ||
          !await pluginBinaryFile.exists()) {
        // Delete the plugin entry from DB if the plugin files are not there.
        await database.pluginsTable.deleteOne(plugin);
        continue;
      }

      pluginConfigs.add(pluginConfig);

      if (plugin.selectedForMetadata) {
        defaultMetadataPlugin = pluginConfigs.length - 1;
      }
      if (plugin.selectedForAudioSource) {
        defaultAudioSourcePlugin = pluginConfigs.length - 1;
      }
    }

    return MetadataPluginState(
      plugins: pluginConfigs,
      defaultMetadataPlugin: defaultMetadataPlugin,
      defaultAudioSourcePlugin: defaultAudioSourcePlugin,
    );
  }

  Future<void> _loadDefaultPlugins(MetadataPluginState pluginState) async {
    const plugins = [
      "spotube-plugin-musicbrainz-listenbrainz",
      "spotube-plugin-youtube-audio",
    ];

    for (final plugin in plugins) {
      final byteData = await rootBundle.load(
        "assets/plugins/$plugin/plugin.smplug",
      );
      final pluginConfig =
          await extractPluginArchive(byteData.buffer.asUint8List());
      try {
        await addPlugin(pluginConfig);
      } on MetadataPluginException catch (e) {
        if (e.errorCode == MetadataPluginErrorCode.duplicatePlugin &&
            await isPluginUpdate(pluginConfig)) {
          final oldConfig = pluginState.plugins
              .firstWhereOrNull((p) => p.slug == pluginConfig.slug);
          if (oldConfig == null) continue;
          final isDefaultMetadata =
              oldConfig == pluginState.defaultMetadataPluginConfig;
          final isDefaultAudioSource =
              oldConfig == pluginState.defaultAudioSourcePluginConfig;

          await removePlugin(pluginConfig);
          await addPlugin(pluginConfig);

          if (isDefaultMetadata) {
            await setDefaultMetadataPlugin(pluginConfig);
          }
          if (isDefaultAudioSource) {
            await setDefaultAudioSourcePlugin(pluginConfig);
          }
        }
      }
    }
  }

  Uri _getGithubReleasesUrl(String repoUrl) {
    final parsedUri = Uri.parse(repoUrl);
    final uri = parsedUri.replace(
      host: "api.github.com",
      pathSegments: [
        "repos",
        ...parsedUri.pathSegments,
        "releases",
      ],
      queryParameters: {
        "per_page": "1",
        "page": "1",
      },
    );

    return uri;
  }

  Uri _getCodebergeReleasesUrl(String repoUrl) {
    final parsedUri = Uri.parse(repoUrl);
    final uri = parsedUri.replace(
      pathSegments: [
        "api",
        "v1",
        "repos",
        ...parsedUri.pathSegments,
        "releases",
      ],
      queryParameters: {
        "limit": "1",
        "page": "1",
      },
    );

    return uri;
  }

  Future<String> _getPluginDownloadUrl(Uri uri) async {
    AppLogger.log.i("Getting plugin download URL from: $uri");
    final res = await globalDio.getUri(
      uri,
      options: Options(responseType: ResponseType.json),
    );

    if (res.statusCode != 200) {
      throw MetadataPluginException.failedToGetRelease();
    }
    final releases = res.data as List;
    if (releases.isEmpty) {
      throw MetadataPluginException.noReleasesFound();
    }
    final latestRelease = releases.first;
    final downloadUrl = (latestRelease["assets"] as List).firstWhere(
      (asset) => (asset["name"] as String).endsWith(".smplug"),
    )["browser_download_url"];
    if (downloadUrl == null) {
      throw MetadataPluginException.assetUrlNotFound();
    }
    return downloadUrl;
  }

  /// Root directory where all metadata plugins are stored.
  Future<Directory> _getPluginRootDir() async => Directory(
        join(
          (await getApplicationSupportDirectory()).path,
          "metadata-plugins",
        ),
      );

  /// Directory where the plugin will be extracted.
  /// This is a unique directory for each plugin version.
  /// It is used to avoid conflicts when multiple versions of the same plugin are installed
  Future<Directory> _getPluginExtractionDir(PluginConfiguration plugin) async {
    final pluginDir = await _getPluginRootDir();
    final pluginExtractionDirPath = join(
      pluginDir.path,
      "${ServiceUtils.sanitizeFilename(plugin.author)}-${ServiceUtils.sanitizeFilename(plugin.name)}-${plugin.version}",
    );
    return Directory(pluginExtractionDirPath);
  }

  Future<PluginConfiguration> extractPluginArchive(List<int> bytes) async {
    final archive = ZipDecoder().decodeBytes(bytes);
    final pluginJson = archive
        .firstWhereOrNull((file) => file.isFile && file.name == "plugin.json");

    if (pluginJson == null) {
      throw MetadataPluginException.pluginConfigJsonNotFound();
    }
    final pluginConfig = PluginConfiguration.fromJson(
      jsonDecode(
        utf8.decode(pluginJson.content as List<int>),
      ) as Map<String, dynamic>,
    );

    final pluginDir = await _getPluginRootDir();
    await pluginDir.create(recursive: true);

    final pluginExtractionDir = await _getPluginExtractionDir(pluginConfig);

    for (final file in archive) {
      if (file.isFile) {
        final filename = file.name;
        final data = file.content as List<int>;
        final extractedFile = File(join(
          pluginExtractionDir.path,
          filename,
        ));
        await extractedFile.create(recursive: true);
        await extractedFile.writeAsBytes(data);
      }
    }

    return pluginConfig;
  }

  /// Downloads, extracts & caches the plugin from the given URL and returns the plugin config.
  /// If only a text/html URL is provided, it will try to get the latest release from
  /// the URL for supported websites (github.com, codeberg.org).
  Future<PluginConfiguration> downloadAndCachePlugin(String url) async {
    final res = await globalDio.head(url);
    final isSupportedWebsite =
        (res.headers["Content-Type"]?.first)?.startsWith("text/html") == true &&
            allowedDomainsRegex.hasMatch(url);
    String pluginDownloadUrl = url;
    if (isSupportedWebsite) {
      if (url.contains("github.com")) {
        final uri = _getGithubReleasesUrl(url);
        pluginDownloadUrl = await _getPluginDownloadUrl(uri);
      } else if (url.contains("codeberg.org")) {
        final uri = _getCodebergeReleasesUrl(url);
        pluginDownloadUrl = await _getPluginDownloadUrl(uri);
      } else {
        throw MetadataPluginException.unsupportedPluginDownloadWebsite();
      }
    }

    // Now let's download, extract and cache the plugin
    final pluginDir = await _getPluginRootDir();
    await pluginDir.create(recursive: true);

    final pluginRes = await globalDio.get(
      pluginDownloadUrl,
      options: Options(
        responseType: ResponseType.bytes,
        followRedirects: true,
        receiveTimeout: const Duration(seconds: 30),
      ),
    );

    if ((pluginRes.statusCode ?? 500) > 299) {
      throw MetadataPluginException.pluginDownloadFailed();
    }

    return await extractPluginArchive(pluginRes.data);
  }

  bool validatePluginApiCompatibility(PluginConfiguration plugin) {
    final configPluginApiVersion = Version.parse(plugin.pluginApiVersion);
    final appPluginApiVersion = MetadataPlugin.pluginApiVersion;

    // Plugin API's major version must match the app's major version
    if (configPluginApiVersion.major != appPluginApiVersion.major) {
      return false;
    }
    return configPluginApiVersion >= appPluginApiVersion;
  }

  void _assertPluginApiCompatibility(PluginConfiguration plugin) {
    if (!validatePluginApiCompatibility(plugin)) {
      throw MetadataPluginException.pluginApiVersionMismatch();
    }
  }

  Future<void> addPlugin(PluginConfiguration plugin) async {
    _assertPluginApiCompatibility(plugin);

    final pluginRes = await (database.pluginsTable.select()
          ..where(
            (tbl) =>
                tbl.name.equals(plugin.name) & tbl.author.equals(plugin.author),
          )
          ..limit(1))
        .get();

    if (pluginRes.isNotEmpty) {
      throw MetadataPluginException.duplicatePlugin();
    }

    await database.pluginsTable.insertOne(
      PluginsTableCompanion.insert(
        name: plugin.name,
        author: plugin.author,
        description: plugin.description,
        version: plugin.version,
        entryPoint: plugin.entryPoint,
        apis: plugin.apis.map((e) => e.name).toList(),
        abilities: plugin.abilities.map((e) => e.name).toList(),
        pluginApiVersion: Value(plugin.pluginApiVersion),
        repository: Value(plugin.repository),
        // Setting the very first plugin as the default plugin
        selectedForMetadata: Value(
          (state.valueOrNull?.plugins
                      .where(
                          (d) => d.abilities.contains(PluginAbilities.metadata))
                      .isEmpty ??
                  true) &&
              plugin.abilities.contains(PluginAbilities.metadata),
        ),
        selectedForAudioSource: Value(
          (state.valueOrNull?.plugins
                      .where((d) =>
                          d.abilities.contains(PluginAbilities.audioSource))
                      .isEmpty ??
                  true) &&
              plugin.abilities.contains(PluginAbilities.audioSource),
        ),
      ),
    );
  }

  Future<void> removePlugin(PluginConfiguration plugin) async {
    final pluginExtractionDir = await _getPluginExtractionDir(plugin);

    if (pluginExtractionDir.existsSync()) {
      await pluginExtractionDir.delete(recursive: true);
    }
    await database.pluginsTable.deleteWhere((tbl) =>
        tbl.name.equals(plugin.name) & tbl.author.equals(plugin.author));

    // Same here, if the removed plugin is the default plugin
    // set the first available plugin as the default plugin
    // only when there is 1 remaining plugin
    if (state.valueOrNull?.defaultMetadataPluginConfig == plugin) {
      final remainingPlugins = state.valueOrNull?.plugins.where(
            (p) =>
                p != plugin && p.abilities.contains(PluginAbilities.metadata),
          ) ??
          [];
      if (remainingPlugins.isNotEmpty) {
        await setDefaultMetadataPlugin(remainingPlugins.first);
      } else {
        await KVStoreService.setDefaultMetadataPlugin("");
      }
    }

    if (state.valueOrNull?.defaultAudioSourcePluginConfig == plugin) {
      final remainingPlugins = state.valueOrNull?.plugins.where(
            (p) =>
                p != plugin &&
                p.abilities.contains(PluginAbilities.audioSource),
          ) ??
          [];
      if (remainingPlugins.isNotEmpty) {
        await setDefaultAudioSourcePlugin(remainingPlugins.first);
      } else {
        await KVStoreService.setDefaultAudioSourcePlugin("");
      }
    }
  }

  Future<bool> isPluginUpdate(PluginConfiguration newPlugin) async {
    final pluginRes = await (database.pluginsTable.select()
          ..where(
            (tbl) =>
                tbl.name.equals(newPlugin.name) &
                tbl.author.equals(newPlugin.author),
          )
          ..limit(1))
        .get();

    if (pluginRes.isEmpty) {
      return false;
    }

    final oldPlugin = pluginRes.first;
    final oldPluginApiVersion = Version.parse(oldPlugin.pluginApiVersion);
    final newPluginApiVersion = Version.parse(newPlugin.pluginApiVersion);

    return newPluginApiVersion > oldPluginApiVersion;
  }

  Future<void> updatePlugin(
    PluginConfiguration plugin,
    PluginUpdateAvailable update,
  ) async {
    final isDefaultMetadata =
        plugin == state.valueOrNull?.defaultMetadataPluginConfig;
    final isDefaultAudioSource =
        plugin == state.valueOrNull?.defaultAudioSourcePluginConfig;
    final pluginUpdatedConfig =
        await downloadAndCachePlugin(update.downloadUrl);

    if (pluginUpdatedConfig.name != plugin.name &&
        pluginUpdatedConfig.author != plugin.author) {
      throw MetadataPluginException.invalidPluginConfiguration();
    }
    _assertPluginApiCompatibility(pluginUpdatedConfig);

    await removePlugin(plugin);
    await addPlugin(pluginUpdatedConfig);

    if (isDefaultMetadata) {
      await setDefaultMetadataPlugin(pluginUpdatedConfig);
    }
    if (isDefaultAudioSource) {
      await setDefaultAudioSourcePlugin(pluginUpdatedConfig);
    }
  }

  Future<void> setDefaultMetadataPlugin(PluginConfiguration plugin) async {
    assert(
      plugin.abilities.contains(PluginAbilities.metadata),
      "Must be a metadata plugin",
    );

    await database.pluginsTable
        .update()
        .write(const PluginsTableCompanion(selectedForMetadata: Value(false)));

    await (database.pluginsTable.update()
          ..where((tbl) =>
              tbl.name.equals(plugin.name) & tbl.author.equals(plugin.author)))
        .write(
      const PluginsTableCompanion(selectedForMetadata: Value(true)),
    );
  }

  Future<void> setDefaultAudioSourcePlugin(PluginConfiguration plugin) async {
    assert(
      plugin.abilities.contains(PluginAbilities.audioSource),
      "Must be an audio-source plugin",
    );

    await database.pluginsTable.update().write(
        const PluginsTableCompanion(selectedForAudioSource: Value(false)));

    await (database.pluginsTable.update()
          ..where((tbl) =>
              tbl.name.equals(plugin.name) & tbl.author.equals(plugin.author)))
        .write(
      const PluginsTableCompanion(selectedForAudioSource: Value(true)),
    );
  }

  Future<Uint8List> getPluginByteCode(PluginConfiguration plugin) async {
    final pluginExtractionDirPath = await _getPluginExtractionDir(plugin);

    final libraryFile = File(join(pluginExtractionDirPath.path, "plugin.out"));

    if (!libraryFile.existsSync()) {
      throw MetadataPluginException.pluginByteCodeFileNotFound();
    }

    return await libraryFile.readAsBytes();
  }

  Future<File?> getLogoPath(PluginConfiguration plugin) async {
    final pluginExtractionDirPath = await _getPluginExtractionDir(plugin);

    final logoFile = File(join(pluginExtractionDirPath.path, "logo.png"));

    if (!logoFile.existsSync()) {
      return null;
    }

    return logoFile;
  }
}

final metadataPluginsProvider =
    AsyncNotifierProvider<MetadataPluginNotifier, MetadataPluginState>(
  MetadataPluginNotifier.new,
);

final metadataPluginProvider = FutureProvider<MetadataPlugin?>(
  (ref) async {
    final defaultPlugin = await ref.watch(
      metadataPluginsProvider
          .selectAsync((data) => data.defaultMetadataPluginConfig),
    );
    final youtubeEngine = ref.read(youtubeEngineProvider);

    if (defaultPlugin == null) {
      return null;
    }

    final pluginsNotifier = ref.read(metadataPluginsProvider.notifier);
    final pluginByteCode =
        await pluginsNotifier.getPluginByteCode(defaultPlugin);

    return await MetadataPlugin.create(
      youtubeEngine,
      defaultPlugin,
      pluginByteCode,
    );
  },
);

final audioSourcePluginProvider = FutureProvider<MetadataPlugin?>(
  (ref) async {
    final defaultPlugin = await ref.watch(
      metadataPluginsProvider
          .selectAsync((data) => data.defaultAudioSourcePluginConfig),
    );
    final youtubeEngine = ref.watch(youtubeEngineProvider);

    if (defaultPlugin == null) {
      return null;
    }

    final pluginsNotifier = ref.read(metadataPluginsProvider.notifier);
    final pluginByteCode =
        await pluginsNotifier.getPluginByteCode(defaultPlugin);

    return await MetadataPlugin.create(
      youtubeEngine,
      defaultPlugin,
      pluginByteCode,
    );
  },
);
