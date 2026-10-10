import 'package:flutter/gestures.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shadcn_flutter/shadcn_flutter.dart';
import 'package:shadcn_flutter/shadcn_flutter_extension.dart';
import 'package:spotube/collections/spotube_icons.dart';
import 'package:spotube/components/markdown/markdown.dart';
import 'package:spotube/extensions/context.dart';
import 'package:spotube/models/metadata/metadata.dart';
import 'package:spotube/provider/metadata_plugin/metadata_plugin_provider.dart';
import 'package:url_launcher/url_launcher_string.dart';
import 'package:change_case/change_case.dart';

final validTopics = {
  "spotube-metadata-plugin": ("Metadata", SpotubeIcons.album),
  "spotube-audio-source-plugin": ("Audio Source", SpotubeIcons.music),
};

class MetadataPluginRepositoryItem extends HookConsumerWidget {
  final MetadataPluginRepository pluginRepo;
  const MetadataPluginRepositoryItem({
    super.key,
    required this.pluginRepo,
  });

  @override
  Widget build(BuildContext context, ref) {
    final pluginsNotifier = ref.watch(metadataPluginsProvider.notifier);
    final host = useMemoized(
      () => Uri.parse(pluginRepo.repoUrl).host,
      [pluginRepo.repoUrl],
    );
    final isInstalling = useState(false);

    return Card(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        spacing: 8,
        children: [
          Basic(
            title: Text(
              pluginRepo.name.startsWith("spotube-plugin")
                  ? pluginRepo.name
                      .replaceFirst("spotube-plugin-", "")
                      .trim()
                      .toCapitalCase()
                  : pluginRepo.name.toCapitalCase(),
            ),
            subtitle: Text(pluginRepo.description),
            trailing: Button.primary(
              enabled: !isInstalling.value,
              onPressed: () async {
                try {
                  isInstalling.value = true;
                  final pluginConfig = await pluginsNotifier
                      .downloadAndCachePlugin(pluginRepo.repoUrl);

                  if (!context.mounted) return;
                  final isOfficialPlugin = ["KRTirtho", "spotube"].contains(pluginRepo.owner);

                  final isAllowed = isOfficialPlugin
                      ? true
                      : await showDialog<bool>(
                          context: context,
                          builder: (context) {
                            final pluginAbilities = pluginConfig.apis
                                .map((e) =>
                                    context.l10n.can_access_name_api(e.name))
                                .join("\n\n");

                            return AlertDialog(
                              title: Text(
                                context.l10n.do_you_want_to_install_this_plugin,
                              ),
                              content: Column(
                                mainAxisSize: MainAxisSize.min,
                                mainAxisAlignment: MainAxisAlignment.start,
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(context.l10n.third_party_plugin_warning),
                                  const Gap(8),
                                  FutureBuilder(
                                    future: pluginsNotifier
                                        .getLogoPath(pluginConfig),
                                    builder: (context, snapshot) {
                                      return Basic(
                                        leading: snapshot.hasData
                                            ? Image.file(
                                                snapshot.data!,
                                                width: 36,
                                                height: 36,
                                              )
                                            : Container(
                                                height: 36,
                                                width: 36,
                                                alignment: Alignment.center,
                                                decoration: BoxDecoration(
                                                  color: context.theme
                                                      .colorScheme.secondary,
                                                  borderRadius:
                                                      BorderRadius.circular(8),
                                                ),
                                                child: const Icon(
                                                    SpotubeIcons.plugin),
                                              ),
                                        title: Text(pluginConfig.name),
                                        subtitle:
                                            Text(pluginConfig.description),
                                      );
                                    },
                                  ),
                                  const Gap(8),
                                  AppMarkdown(
                                    data:
                                        "**${context.l10n.author}**: ${pluginConfig.author}\n\n"
                                        "**${context.l10n.repository}**: [${pluginConfig.repository ?? 'N/A'}](${pluginConfig.repository})\n\n\n\n"
                                        "${context.l10n.this_plugin_can_do_following}:\n\n"
                                        "$pluginAbilities",
                                  ),
                                ],
                              ),
                              actions: [
                                Button.secondary(
                                  onPressed: () {
                                    Navigator.of(context).pop(false);
                                  },
                                  child: Text(context.l10n.decline),
                                ),
                                Button.primary(
                                  onPressed: () {
                                    Navigator.of(context).pop(true);
                                  },
                                  child: Text(context.l10n.accept),
                                ),
                              ],
                            );
                          },
                        );

                  if (isAllowed != true) return;
                  await pluginsNotifier.addPlugin(pluginConfig);
                } finally {
                  if (context.mounted) {
                    isInstalling.value = false;
                  }
                }
              },
              leading: isInstalling.value
                  ? SizedBox.square(
                      dimension: 20,
                      child: CircularProgressIndicator(
                        color: context.theme.colorScheme.primaryForeground,
                      ),
                    )
                  : const Icon(SpotubeIcons.add),
              child: Text(context.l10n.install),
            ),
          ),
          if (!["KRTirtho", "spotube"].contains(pluginRepo.owner))
            Text.rich(
              TextSpan(
                children: [
                  TextSpan(text: context.l10n.source),
                  TextSpan(
                    text: pluginRepo.repoUrl.replaceAll("https://", ""),
                    style: const TextStyle(
                      color: Colors.blue,
                      decoration: TextDecoration.underline,
                    ),
                    recognizer: TapGestureRecognizer()
                      ..onTap = () async {
                        launchUrlString(pluginRepo.repoUrl);
                      },
                  ),
                ],
              ),
              style: context.theme.typography.xSmall,
            ),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              if (["KRTirtho", "spotube"].contains(pluginRepo.owner))
                PrimaryBadge(
                  leading: const Icon(SpotubeIcons.done),
                  child: Text(context.l10n.official),
                )
              else ...[
                Text(
                  context.l10n.author_name(pluginRepo.owner),
                  style: context.theme.typography.xSmall,
                ),
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
              for (final topic in pluginRepo.topics)
                if (validTopics.keys.contains(topic))
                  SecondaryBadge(
                    leading: Icon(validTopics[topic]!.$2),
                    child: Text(validTopics[topic]!.$1),
                  ),
              SecondaryBadge(
                leading: host == "github.com"
                    ? const Icon(SpotubeIcons.github)
                    : null,
                child: Text(host),
                onPressed: () {
                  launchUrlString(pluginRepo.repoUrl);
                },
              ),
            ],
          ),
        ],
      ),
    );
  }
}
