import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shadcn_flutter/shadcn_flutter.dart';
import 'package:shadcn_flutter/shadcn_flutter_extension.dart';
import 'package:spotube/collections/spotube_icons.dart';
import 'package:spotube/components/markdown/markdown.dart';
import 'package:spotube/extensions/constrains.dart';
import 'package:spotube/extensions/context.dart';
import 'package:spotube/models/metadata/metadata.dart';
import 'package:spotube/modules/metadata_plugins/plugin_update_available_dialog.dart';
import 'package:spotube/provider/metadata_plugin/core/auth.dart';
import 'package:spotube/provider/metadata_plugin/core/support.dart';
import 'package:spotube/provider/metadata_plugin/metadata_plugin_provider.dart';
import 'package:spotube/provider/metadata_plugin/updater/update_checker.dart';
import 'package:url_launcher/url_launcher.dart';

final validAbilities = {
  PluginAbilities.metadata: ("Metadata", SpotubeIcons.album),
  PluginAbilities.audioSource: ("Audio Source", SpotubeIcons.music),
};

class MetadataInstalledPluginItem extends HookConsumerWidget {
  final PluginConfiguration plugin;
  final bool isDefaultMetadata;
  final bool isDefaultAudioSource;
  const MetadataInstalledPluginItem({
    super.key,
    required this.plugin,
    required this.isDefaultMetadata,
    required this.isDefaultAudioSource,
  });

  @override
  Widget build(BuildContext context, ref) {
    final mediaQuery = MediaQuery.sizeOf(context);

    final metadataPlugin = ref.watch(metadataPluginProvider);
    final audioSourcePlugin = ref.watch(audioSourcePluginProvider);
    final pluginSnapshot = switch ((isDefaultMetadata, isDefaultAudioSource)) {
      (true, _) => metadataPlugin,
      (false, true) => audioSourcePlugin,
      _ => null,
    };

    final pluginsNotifier = ref.watch(metadataPluginsProvider.notifier);

    final requiresAuth = (isDefaultMetadata || isDefaultAudioSource) &&
        plugin.abilities.contains(PluginAbilities.authentication);
    final supportsScrobbling = isDefaultMetadata &&
        plugin.abilities.contains(PluginAbilities.scrobbling);

    final isMetadataAuthenticatedSnapshot =
        ref.watch(metadataPluginAuthenticatedProvider);
    final isAudioSourceAuthenticatedSnapshot =
        ref.watch(audioSourcePluginAuthenticatedProvider);
    final isAuthenticated = (isDefaultMetadata &&
            isMetadataAuthenticatedSnapshot.asData?.value == true) ||
        (isDefaultAudioSource &&
            isAudioSourceAuthenticatedSnapshot.asData?.value == true);

    final metadataUpdateAvailable =
        ref.watch(metadataPluginUpdateCheckerProvider);
    final audioSourceUpdateAvailable =
        ref.watch(audioSourcePluginUpdateCheckerProvider);
    final updateAvailable = switch ((isDefaultMetadata, isDefaultAudioSource)) {
      (true, _) => metadataUpdateAvailable,
      (false, true) => audioSourceUpdateAvailable,
      _ => null,
    };
    final hasUpdate = updateAvailable?.asData?.value != null;

    return Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        spacing: 12,
        children: [
          FutureBuilder(
            future: pluginsNotifier.getLogoPath(plugin),
            builder: (context, snapshot) {
              final repoUrl = plugin.repository != null
                  ? Uri.tryParse(plugin.repository!)
                  : null;
              final repoOwner = repoUrl?.pathSegments.firstOrNull;

              final isOfficial =
                  repoUrl?.host == "github.com" && ["KRTirtho", "spotube"].contains(repoOwner);

              return Basic(
                leading: snapshot.hasData
                    ? ClipRRect(
                        borderRadius: BorderRadius.circular(8),
                        child: Image.file(
                          snapshot.data!,
                          width: 36,
                          height: 36,
                        ),
                      )
                    : Container(
                        height: 36,
                        width: 36,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: context.theme.colorScheme.secondary,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Icon(SpotubeIcons.plugin),
                      ),
                title: Text(plugin.name),
                subtitle: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  spacing: 8,
                  children: [
                    Text(plugin.description),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        for (final ability in plugin.abilities)
                          if (validAbilities.keys.contains(ability))
                            SecondaryBadge(
                              leading: Icon(validAbilities[ability]!.$2),
                              child: Text(validAbilities[ability]!.$1),
                            ),
                      ],
                    ),
                    if (repoUrl != null)
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          if (isOfficial)
                            PrimaryBadge(
                              leading: const Icon(SpotubeIcons.done),
                              child: Text(context.l10n.official),
                            )
                          else ...[
                            Text(context.l10n.author_name(plugin.author)),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 6,
                                vertical: 2,
                              ),
                              decoration: BoxDecoration(
                                color: Colors.blue,
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                spacing: 4,
                                children: [
                                  const Icon(SpotubeIcons.warning, size: 14),
                                  Text(
                                    context.l10n.third_party,
                                    style: const TextStyle(color: Colors.white),
                                  ).xSmall
                                ],
                              ),
                            ),
                          ],
                          SecondaryBadge(
                            leading: const Icon(SpotubeIcons.connect),
                            child: Text(repoUrl.host),
                            onPressed: () {
                              launchUrl(repoUrl);
                            },
                          ),
                          SecondaryBadge(
                            child: Padding(
                              padding: const EdgeInsets.all(1),
                              child: Text(
                                "${context.l10n.version}: ${plugin.version}",
                              ),
                            ),
                          ),
                        ],
                      )
                  ],
                ),
                trailing: IconButton.ghost(
                  onPressed: () async {
                    await pluginsNotifier.removePlugin(plugin);
                  },
                  icon: const Icon(
                    SpotubeIcons.trash,
                    color: Colors.red,
                  ),
                ),
              );
            },
          ),
          if ((requiresAuth && !isAuthenticated) ||
              hasUpdate ||
              supportsScrobbling)
            Container(
              decoration: BoxDecoration(
                color: context.theme.colorScheme.secondary,
                borderRadius: BorderRadius.circular(8),
              ),
              padding: const EdgeInsets.all(12),
              child: Column(
                spacing: 12,
                children: [
                  if (requiresAuth && !isAuthenticated)
                    Row(
                      spacing: 8,
                      children: [
                        const Icon(SpotubeIcons.warning, color: Colors.yellow),
                        Text(context.l10n.plugin_requires_authentication),
                      ],
                    ),
                  if (hasUpdate)
                    SizedBox(
                      width: double.infinity,
                      child: Basic(
                        leading: const Icon(SpotubeIcons.update),
                        title: Text(context.l10n.update_available),
                        subtitle: Text(
                          updateAvailable!.asData!.value!.version,
                        ),
                        trailing: Button.primary(
                          onPressed: () {
                            showDialog(
                              context: context,
                              builder: (context) =>
                                  MetadataPluginUpdateAvailableDialog(
                                plugin: plugin,
                                update: updateAvailable.asData!.value!,
                              ),
                            );
                          },
                          child: Text(context.l10n.update),
                        ),
                      ),
                    ),
                  if (supportsScrobbling)
                    SizedBox(
                      width: double.infinity,
                      child: Basic(
                        leading: const Icon(SpotubeIcons.info),
                        title: Text(context.l10n.supports_scrobbling),
                        subtitle: Text(context.l10n.plugin_scrobbling_info),
                      ),
                    )
                ],
              ),
            ),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            alignment: WrapAlignment.spaceBetween,
            children: [
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  if (plugin.abilities.contains(PluginAbilities.metadata))
                    Button.secondary(
                      enabled: !isDefaultMetadata,
                      onPressed: () async {
                        await pluginsNotifier.setDefaultMetadataPlugin(plugin);
                      },
                      child: Text(
                        isDefaultMetadata
                            ? context.l10n.default_metadata_source
                            : context.l10n.set_default_metadata_source,
                      ),
                    ),
                  if (plugin.abilities.contains(PluginAbilities.audioSource))
                    Button.secondary(
                      enabled: !isDefaultAudioSource,
                      onPressed: () async {
                        await pluginsNotifier
                            .setDefaultAudioSourcePlugin(plugin);
                      },
                      child: Text(
                        isDefaultAudioSource
                            ? context.l10n.default_audio_source
                            : context.l10n.set_default_audio_source,
                      ),
                    ),
                ],
              ),
              Row(
                mainAxisSize:
                    mediaQuery.smAndUp ? MainAxisSize.min : MainAxisSize.max,
                mainAxisAlignment: MainAxisAlignment.end,
                spacing: 8,
                children: [
                  if (isDefaultMetadata || isDefaultAudioSource)
                    Consumer(builder: (context, ref, _) {
                      final metadataSupportTextSnapshot =
                          ref.watch(metadataPluginSupportTextProvider);
                      final audioSourceSupportTextSnapshot =
                          ref.watch(audioSourcePluginSupportTextProvider);

                      final supportTextSnapshot =
                          switch ((isDefaultMetadata, isDefaultAudioSource)) {
                        (true, _) => metadataSupportTextSnapshot,
                        (false, true) => audioSourceSupportTextSnapshot,
                        _ => null,
                      };

                      if ((supportTextSnapshot?.hasValue ?? false) &&
                          supportTextSnapshot?.value == null) {
                        return const SizedBox.shrink();
                      }

                      final bgColor =
                          context.theme.brightness == Brightness.dark
                              ? const Color.fromARGB(255, 255, 145, 175)
                              : Colors.pink[600];
                      final textColor =
                          context.theme.brightness == Brightness.dark
                              ? Colors.pink[700]
                              : Colors.pink[50];

                      final mediaQuery = MediaQuery.sizeOf(context);

                      return Button(
                        style: ButtonVariance.secondary.copyWith(
                          decoration: (context, states, value) {
                            return value.copyWithIfBoxDecoration(
                              color: bgColor,
                            );
                          },
                          textStyle: (context, states, value) {
                            return value.copyWith(
                              color: textColor,
                            );
                          },
                          iconTheme: (context, states, value) {
                            return value.copyWith(
                              color: textColor,
                            );
                          },
                        ),
                        leading: const Icon(SpotubeIcons.heartFilled),
                        child: Text(context.l10n.support),
                        onPressed: () {
                          showDialog(
                            context: context,
                            builder: (context) {
                              return AlertDialog(
                                title: Text(
                                    context.l10n.support_plugin_development),
                                content: ConstrainedBox(
                                  constraints: BoxConstraints(
                                    maxHeight: mediaQuery.height * 0.8,
                                    maxWidth: 720,
                                  ),
                                  child: SizedBox(
                                    width: double.infinity,
                                    child: SingleChildScrollView(
                                      child: AppMarkdown(
                                        data: supportTextSnapshot
                                                ?.asData?.value ??
                                            "",
                                      ),
                                    ),
                                  ),
                                ),
                                actions: [
                                  Button.secondary(
                                    onPressed: () {
                                      Navigator.of(context).pop();
                                    },
                                    child: Text(context.l10n.close),
                                  ),
                                ],
                              );
                            },
                          );
                        },
                      );
                    }),
                  if ((isDefaultMetadata || isDefaultAudioSource) &&
                      requiresAuth &&
                      !isAuthenticated)
                    Button.primary(
                      onPressed: () async {
                        await pluginSnapshot?.asData?.value?.auth
                            .authenticate();
                      },
                      leading: const Icon(SpotubeIcons.login),
                      child: Text(context.l10n.login),
                    )
                  else if ((isDefaultMetadata || isDefaultAudioSource) &&
                      requiresAuth &&
                      isAuthenticated)
                    Button.destructive(
                      onPressed: () async {
                        await pluginSnapshot?.asData?.value?.auth.logout();
                      },
                      leading: const Icon(SpotubeIcons.logout),
                      child: Text(context.l10n.logout),
                    ),
                ],
              )
            ],
          )
        ],
      ),
    );
  }
}
