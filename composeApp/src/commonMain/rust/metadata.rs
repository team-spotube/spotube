use lofty::config::WriteOptions;
use lofty::error::{ErrorKind, LoftyError};
use lofty::file::{AudioFile, FileType, TaggedFile, TaggedFileExt};
use lofty::picture::{Picture, PictureType};
use lofty::probe::Probe;
use lofty::tag::{Accessor, Tag, TagExt};
use std::path::Path;

#[derive(uniffi::Record)]
pub struct AudioMetadata {
    pub title: Option<String>,
    pub artists: Vec<String>,
    pub album: Option<String>,
    pub duration_ms: i64,
    pub track_number: Option<u32>,
    pub disc_number: Option<u32>,
    pub cover_bytes: Option<Vec<u8>>,
}

#[derive(Debug, uniffi::Error)]
pub enum AudioTagError {
    FileNotFound,
    AccessDenied,
    UnsupportedFormat,
    UnsupportedCover,
    ReadError { reason: String },
    WriteError { reason: String },
}

impl std::fmt::Display for AudioTagError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            AudioTagError::FileNotFound => write!(f, "File not found"),
            AudioTagError::AccessDenied => write!(f, "Cannot write to the file"),
            AudioTagError::UnsupportedFormat => write!(f, "Unsupported audio tag format"),
            AudioTagError::UnsupportedCover => write!(f, "Unsupported cover image"),
            AudioTagError::ReadError { reason } => write!(f, "Read error: {}", reason),
            AudioTagError::WriteError { reason } => write!(f, "Write error: {}", reason),
        }
    }
}

#[uniffi::export]
pub fn read_audio_metadata(file_path: String) -> Result<AudioMetadata, AudioTagError> {
    let path = Path::new(&file_path);
    if !path.exists() {
        return Err(AudioTagError::FileNotFound);
    }

    let tagged_file = Probe::open(path)
        .map_err(|e| tag_format_error(e, false))?
        .read()
        .map_err(|e| tag_format_error(e, false))?;

    Ok(extract_audio_metadata(&tagged_file))
}

fn extract_audio_metadata(tagged_file: &TaggedFile) -> AudioMetadata {
    let duration = tagged_file.properties().duration();
    let duration_ms = duration.as_millis() as i64;

    let tag = tagged_file.primary_tag().or_else(|| tagged_file.first_tag());

    let mut title = None;
    let mut artists = Vec::new();
    let mut album = None;
    let mut track_number = None;
    let mut disc_number = None;
    let mut cover_bytes = None;

    if let Some(tag) = tag {
        title = tag.title().map(|s| s.to_string());

        if let Some(artist) = tag.artist() {
            artists = artist
                .split(';')
                .map(|s| s.trim().to_string())
                .filter(|s| !s.is_empty())
                .collect();
        }

        album = tag.album().map(|s| s.to_string());
        track_number = tag.track();
        disc_number = tag.disk();

        if let Some(pic) = tag.pictures().first() {
            cover_bytes = Some(pic.data().to_vec());
        }
    }

    AudioMetadata {
        title,
        artists,
        album,
        duration_ms,
        track_number,
        disc_number,
        cover_bytes,
    }
}

#[uniffi::export]
pub fn write_audio_metadata(
    file_path: String,
    title: String,
    artists: String,
    album: Option<String>,
    track_number: Option<i32>,
    disc_number: Option<i32>,
    cover_bytes: Option<Vec<u8>>,
) -> Result<(), AudioTagError> {
    let path = Path::new(&file_path);
    if !path.exists() {
        return Err(AudioTagError::FileNotFound);
    }

    let tagged_file = Probe::open(path)
        .map_err(|e| tag_format_error(e, false))?
        .read()
        .map_err(|e| tag_format_error(e, false))?;

    let tag = updated_audio_tag(tagged_file, title, artists, album, track_number, disc_number, cover_bytes)?;
    tag.save_to_path(path, WriteOptions::default())
        .map_err(|e| tag_format_error(e, true))
}

#[uniffi::export]
pub fn read_audio_metadata_from_descriptor(descriptor: i32, display_name: String) -> Result<AudioMetadata, AudioTagError> {
    #[cfg(unix)]
    {
        let mut file = duplicate_audio_descriptor(descriptor)?;
        let tagged = read_descriptor_tags(&mut file, &display_name)?;
        Ok(extract_audio_metadata(&tagged))
    }
    #[cfg(not(unix))]
    {
        let _ = (descriptor, display_name);
        Err(AudioTagError::ReadError { reason: "File descriptors are not supported on this platform".into() })
    }
}

/// Edit a file on a seekable descriptor obtained from Android's MediaStore. Reopening its
/// filesystem path would lose the provider's per-item write grant.
#[uniffi::export]
pub fn write_audio_metadata_to_descriptor(
    descriptor: i32,
    display_name: String,
    title: String,
    artists: String,
    album: Option<String>,
    track_number: Option<i32>,
    disc_number: Option<i32>,
    cover_bytes: Option<Vec<u8>>,
) -> Result<(), AudioTagError> {
    #[cfg(unix)]
    {
        use std::io::Seek;
        let mut file = duplicate_audio_descriptor(descriptor)?;
        let info = read_descriptor_tags(&mut file, &display_name)?;
        let tag = updated_audio_tag(info, title, artists, album, track_number, disc_number, cover_bytes)?;
        file.rewind().map_err(|e| AudioTagError::WriteError { reason: e.to_string() })?;
        tag.save_to(&mut file, WriteOptions::default())
            .map_err(|e| tag_format_error(e, true))
    }
    #[cfg(not(unix))]
    {
        let _ = (descriptor, display_name, title, artists, album, track_number, disc_number, cover_bytes);
        Err(AudioTagError::WriteError { reason: "File descriptors are not supported on this platform".into() })
    }
}

#[cfg(unix)]
fn duplicate_audio_descriptor(descriptor: i32) -> Result<std::fs::File, AudioTagError> {
    use std::os::fd::FromRawFd;
    if descriptor < 0 {
        return Err(AudioTagError::FileNotFound);
    }
    // Validate and duplicate the OS handle without taking ownership of the caller's handle.
    let duplicate = unsafe { libc::fcntl(descriptor, libc::F_DUPFD_CLOEXEC, 0) };
    if duplicate < 0 {
        return Err(AudioTagError::ReadError { reason: std::io::Error::last_os_error().to_string() });
    }
    Ok(unsafe { std::fs::File::from_raw_fd(duplicate) })
}

#[cfg(unix)]
fn read_descriptor_tags(file: &mut std::fs::File, display_name: &str) -> Result<TaggedFile, AudioTagError> {
    use std::io::{BufReader, Seek};
    file.rewind().map_err(|e| AudioTagError::ReadError { reason: e.to_string() })?;
    let reader = BufReader::new(file);
    let probe = if let Some(format) = FileType::from_path(display_name) {
        Probe::with_file_type(reader, format)
    } else {
        Probe::new(reader).guess_file_type()
            .map_err(|e| AudioTagError::ReadError { reason: e.to_string() })?
    };
    probe.read().map_err(|e| tag_format_error(e, false))
}

fn tag_format_error(error: LoftyError, writing: bool) -> AudioTagError {
    match error.kind() {
        ErrorKind::UnknownFormat | ErrorKind::UnsupportedTag => AudioTagError::UnsupportedFormat,
        ErrorKind::Io(e) if e.kind() == std::io::ErrorKind::PermissionDenied => AudioTagError::AccessDenied,
        _ if writing => AudioTagError::WriteError { reason: error.to_string() },
        _ => AudioTagError::ReadError { reason: error.to_string() },
    }
}

fn updated_audio_tag(
    tagged_file: TaggedFile,
    title: String,
    artists: String,
    album: Option<String>,
    track_number: Option<i32>,
    disc_number: Option<i32>,
    cover_bytes: Option<Vec<u8>>,
) -> Result<Tag, AudioTagError> {
    // An old MP3 can have only ID3v1. Updating that tag truncates titles and cannot save art.
    // Always use the container's primary format, carrying over the existing mapped fields.
    let tag_type = tagged_file.primary_tag_type();
    let mut tag = tagged_file.primary_tag().or_else(|| tagged_file.first_tag()).cloned()
        .unwrap_or_else(|| Tag::new(tag_type));
    if tag.tag_type() != tag_type {
        tag.re_map(tag_type);
    }

    tag.set_title(title);
    tag.set_artist(artists);

    if let Some(album_name) = album {
        tag.set_album(album_name);
    } else {
        tag.remove_album();
    }

    if let Some(num) = track_number {
        tag.set_track(num as u32);
    } else {
        tag.remove_track();
    }

    if let Some(num) = disc_number {
        tag.set_disk(num as u32);
    } else {
        tag.remove_disk();
    }

    if let Some(bytes) = cover_bytes {
        let mut picture = Picture::from_reader(&mut bytes.as_slice()).map_err(|_| AudioTagError::UnsupportedCover)?;
        picture.set_pic_type(PictureType::CoverFront);
        while !tag.pictures().is_empty() {
            tag.remove_picture(0);
        }
        tag.push_picture(picture);
    }

    Ok(tag)
}

#[cfg(all(test, unix))]
mod tests {
    use super::*;
    use std::fs::OpenOptions;
    use std::io::{Seek, Write};
    use std::os::fd::AsRawFd;

    #[test]
    fn writes_tags_on_an_open_file_descriptor_without_closing_it() {
        let unique = std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_nanos();
        let path = std::env::temp_dir().join(format!("spotube-metadata-{}-{unique}.wav", std::process::id()));
        let mut file = OpenOptions::new().read(true).write(true).create_new(true).open(&path).unwrap();

        // One 16-bit mono sample at 8 kHz, enough for a valid waveform container.
        file.write_all(b"RIFF\x26\x00\x00\x00WAVEfmt \x10\x00\x00\x00\x01\x00\x01\x00\x40\x1f\x00\x00\x80\x3e\x00\x00\x02\x00\x10\x00data\x02\x00\x00\x00\x00\x00").unwrap();
        let result = write_audio_metadata_to_descriptor(file.as_raw_fd(), "test.wav".into(),
            "Permission test".into(), "Spotube".into(), None, None, None, None);
        let observed = read_audio_metadata(path.to_string_lossy().into_owned());
        let from_descriptor = read_audio_metadata_from_descriptor(file.as_raw_fd(), "test.wav".into());
        let handle_still_open = file.rewind().is_ok();
        std::fs::remove_file(path).unwrap();

        result.unwrap();
        assert_eq!(observed.unwrap().title.as_deref(), Some("Permission test"));
        assert_eq!(from_descriptor.unwrap().title.as_deref(), Some("Permission test"));
        assert!(handle_still_open);
    }

    #[test]
    fn promotes_old_mp3_tags_to_id3v2_instead_of_truncating_the_title() {
        use lofty::properties::FileProperties;
        use lofty::tag::TagType;
        let file = TaggedFile::new(FileType::Mpeg, FileProperties::default(), vec![Tag::new(TagType::Id3v1)]);
        let title = "This is a full-length title that does not fit in ID3v1";
        let tag = updated_audio_tag(file, title.into(), "Artist".into(), Some("Album".into()), Some(2), Some(1), None).unwrap();
        assert_eq!(tag.tag_type(), TagType::Id3v2);
        assert_eq!(tag.title().as_deref(), Some(title));
        assert_eq!(tag.disk(), Some(1));
    }

    #[test]
    fn unsupported_container_leaves_the_file_unchanged() {
        let unique = std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_nanos();
        let path = std::env::temp_dir().join(format!("spotube-metadata-{}-{unique}.wma", std::process::id()));
        let original = b"\x30\x26\xb2\x75\x8e\x66\xcf\x11\xa6\xd9\x00\xaa\x00\x62\xce\x6c"; // ASF header GUID
        let mut file = OpenOptions::new().read(true).write(true).create_new(true).open(&path).unwrap();
        file.write_all(original).unwrap();
        let result = write_audio_metadata_to_descriptor(file.as_raw_fd(), "test.wma".into(),
            "Permission test".into(), "Spotube".into(), None, None, None, None);
        let observed = std::fs::read(&path).unwrap();
        std::fs::remove_file(path).unwrap();

        assert!(matches!(result, Err(AudioTagError::UnsupportedFormat)));
        assert_eq!(observed, original);
    }
}
