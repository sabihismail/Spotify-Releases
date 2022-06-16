package models

enum class SpotifyStatus {
    NOT_STARTED,
    AUTHENTICATED,
    PLAYLIST_FETCH_START,
    ARTIST_FETCH_START,
    ALBUM_FETCH_START,
    COMPLETED,
}