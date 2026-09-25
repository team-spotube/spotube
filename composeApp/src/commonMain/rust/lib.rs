mod metadata;
mod discord_rpc;

pub use metadata::*;
pub use discord_rpc::*;

uniffi::setup_scaffolding!();