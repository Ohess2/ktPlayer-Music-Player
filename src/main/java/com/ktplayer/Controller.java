package com.ktplayer;

import com.jfoenix.controls.JFXSlider;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.media.MediaPlayer.Status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;



public class Controller {

    @FXML
    private AnchorPane window;

    @FXML
    private AnchorPane playlistNode;

    @FXML
    private Pane showPlaylist;
    @FXML
    private Pane exit;
    @FXML
    private Pane minimize;
    @FXML
    private Pane imagePane;

    @FXML
    private TableView<Song> songTable;
    @FXML
    private TableColumn<Song, String> idColumn;
    @FXML
    private TableColumn<Song, String> artistNameColumn;
    @FXML
    private TableColumn<Song, String> songNameColumn;
    @FXML
    private TableColumn<Song, String> durationColumn;
    @FXML
    private TableColumn<Song, String> rateColumn;
    @FXML
    private TableColumn<Song, String> formatColumn;


    @FXML
    private Label artistName;
    @FXML
    private Label albumName;
    @FXML
    private Label songName;
    @FXML
    private Label totalDuration;
    @FXML
    private Label currentDuration;
    @FXML
    private Label volumeValue;
    @FXML
    private Label songsCounter;

    @FXML
    private JFXSlider songSlider;
    @FXML
    private Slider volumeSlider;

    @FXML
    private ImageView folderChooser;

    @FXML
    private ImageView playButton;
    @FXML
    private ImageView pauseButton;
    @FXML
    private ImageView nextSongButton;
    @FXML
    private ImageView previousSongButton;
    @FXML
    private ImageView muteIcon;
    @FXML
    private ImageView volumeIcon;
    @FXML
    private ToggleButton autoPlayIcon;


    @FXML
    private Stage stage;

    private Main main;

    private List<MediaPlayer> players;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;

    private boolean isAutoplay;
    private double volume = 10;
    private String path;

    private double xOffset = 0;
    private double yOffset = 0;

    private FadeTransition fadeIn = new FadeTransition();
    private FadeTransition fadeOut = new FadeTransition();

    // Album art is drawn in an ImageView inside imagePane (not as the pane's CSS background,
    // because the #imagePane rules in LightTheme.css would override a background set from code)
    private final ImageView albumArtView = new ImageView();
    private Image placeholderArt;
    // Decoded covers keyed by song path (or folder path for folder images). A null value means
    // "looked, found nothing". Capped so browsing a huge library doesn't keep every cover in memory.
    private final Map<String, Image> albumArtCache = new LinkedHashMap<String, Image>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > ALBUM_ART_CACHE_SIZE;
        }
    };


    public Controller() {
        players = new ArrayList<>();
        songSlider = new JFXSlider();
        isAutoplay = false;
        volume = 0.1;
        stage = Main.getStage();
        stage.getIcons().add(new Image(ClassLoader.getSystemResource("images/logo.png").toExternalForm()));
    }

    @FXML
    private void initialize() throws Exception {
        setUpAlbumArtView();

        window.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = stage.getX() - event.getScreenX();
                yOffset = stage.getY() - event.getScreenY();
            }
        });

        window.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                stage.setX(event.getScreenX() + xOffset);
                stage.setY(event.getScreenY() + yOffset);
            }
        });

        autoPlayIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(isAutoplay) {
                    autoPlayIcon.setSelected(false);
                    isAutoplay = false;
                }
                else if(!isAutoplay) {
                    autoPlayIcon.setSelected(true);
                    isAutoplay = true;
                }
            }
        });

        showPlaylist.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(playlistNode.isVisible() == true) {
                    hideTransition(playlistNode);
                }
                else {
                    showTransition(playlistNode);
                }
            }
        });

        minimize.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                stage.setIconified(true);
            }
        });

        exit.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.exit(0);
            }
        });

        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        artistNameColumn.setCellValueFactory(cellData -> cellData.getValue().artistNameProperty());
        songNameColumn.setCellValueFactory(cellData -> cellData.getValue().songNameProperty());
        durationColumn.setCellValueFactory(cellData -> cellData.getValue().durationProperty());
        rateColumn.setCellValueFactory(cellData -> cellData.getValue().rateProperty());
        formatColumn.setCellValueFactory(cellData -> cellData.getValue().formatProperty());

        showSongInfo(null);

        songTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> showSongInfo(newValue));

        folderChooser.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                DirectoryChooser chooser = new DirectoryChooser();
                File selectedDirectory = chooser.showDialog(stage);
                if(selectedDirectory == null) {
                    System.out.println("No directory selected!");
                }
                else {

                    try {
                        if(!(players.isEmpty())) {
                            players.clear();
                            System.out.println("new array list");
                        }
                        songTable.setItems(songsUrls(selectedDirectory));

                        songTable.setOnMouseClicked((MouseEvent e) -> {
                            if((e.getClickCount() == 1)) {
                                try {
                                    takeCare();
                                }
                                catch (Exception ex) {};
                            }
                        });
                    }
                    catch(Exception e) {
                        System.out.println(e.toString());
                    }
                }
            }
        });
    }

    public void showSongInfo(Song song) {
        if(song != null) {
            artistName.setText(song.getArtistName());
            songName.setText(song.getSongName());
            albumName.setText(song.getFormat());
            showAlbumArt(new File(song.getUrl()));
        }
        else {
            artistName.setText("-");
            songName.setText("-");
            albumName.setText("-");
            showAlbumArt(null);
        }

    }

    public ObservableList<Song> songsUrls(File dir) throws Exception{ // FILES HANDLED HERE. returns list of Song objects from the directory
        ObservableList<Song> songData = FXCollections.observableArrayList();
        File[] files = dir.listFiles();
        String name;
        int i = 0;
        for(File file : files) {
            if(file.isFile()) {
                name = file.getName();
                /*if(name.endsWith("jpg")) {
                    path = file.getAbsolutePath();
                }*/
                if(isPlayable(name)) {
                    try {
                        // jaudiotagger reads tags + length for mp3, wav, aiff, m4a, flac, ogg, etc.
                        AudioFile audioFile = AudioFileIO.read(file);
                        Tag tag = audioFile.getTag(); // may be null (e.g. untagged WAV)
                        long seconds = audioFile.getAudioHeader().getTrackLength();
                        i++;
                        Song song = new Song(String.valueOf(i),
                                tagOr(tag, FieldKey.ARTIST, "Unknown"),
                                tagOr(tag, FieldKey.TITLE, stripExtension(name)),
                                kbToMb(file.length()), secToMin(seconds),
                                tagOr(tag, FieldKey.ALBUM, "-"),
                                file.getAbsolutePath());
                        players.add(createPlayer(file.getAbsolutePath())); // controls for playing media, from javafx
                        songData.add(song);
                    }
                    catch(Exception e) {
                        System.out.println("Skipping " + name + ": " + e);
                    }
                }
            }
        }
        showAlbumArt(null); // new folder loaded, nothing selected yet - show the placeholder
        i = 0;
        System.out.println(players.size());
        songsCounter.setText("");
        songsCounter.setText("Songs: " + players.size());
        return songData;
    }

    public void playPauseSong(Song song) throws Exception {
        if(song != null) {
            File file = new File(song.getUrl());
            String path = file.getAbsolutePath();
            path.replace("\\", "/");

            if((mediaView != null) && (mediaPlayer != null)) { // get rid of the current media player if there is one
                volume = mediaView.getMediaPlayer().getVolume();
                mediaView.getMediaPlayer().stop();
                mediaView = null;
                mediaPlayer = null;
            }

            Media media = new Media(new File(path).toURI().toString()); // make Media object for the song

            mediaPlayer = new MediaPlayer(media); // make a new mediaplayer for the Media object for the song
            mediaPlayer.stop(); // don't play yet
            mediaPlayer.setAutoPlay(false);

            mediaView = new MediaView(mediaPlayer); // make the GUI component
            pauseIcon();
            mediaView = new MediaView(players.get(Integer.parseInt(song.getId()) - 1));

            volumeValue.setText(String.valueOf((int)volumeSlider.getValue()));
            volumeSlider.setValue(volume * 100);
            mediaView.getMediaPlayer().setVolume(volume);
            mediaView.getMediaPlayer().seek(Duration.ZERO);
            updateSliderPosition(Duration.ZERO);

            updateValues();
            mediaView.mediaPlayerProperty().addListener(new ChangeListener<MediaPlayer>() {
                @Override
                public void changed(ObservableValue<? extends MediaPlayer> observable, MediaPlayer oldValue, MediaPlayer newValue) {
                    try {
                        setCurrentlyPlayer(newValue);
                        updateValues();
                    }
                    catch(Exception e) {
                        System.out.println(e.toString());
                    }
                }
            });


            playButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    mediaView.getMediaPlayer().play();
                    playIcon();
                    updateValues();
                    for (int i = ((players.indexOf(mediaView.getMediaPlayer())) % players.size()); i < players.size(); i++) {
                        final MediaPlayer player = players.get(i);
                        mediaPlayer = player;
                        final MediaPlayer nextPlayer = players.get((i + 1) % players.size());
                        mediaPlayer.setOnEndOfMedia(new Runnable() {
                            @Override
                            public void run() {
                                mediaView.getMediaPlayer().stop();
                                mediaView.getMediaPlayer().seek(Duration.ZERO);
                                if(isAutoplay) {
                                    mediaView.getMediaPlayer().seek(Duration.ZERO);
                                    repeatSongs();
                                    return;
                                }
                                mediaPlayer = nextPlayer;
                                mediaView.setMediaPlayer(mediaPlayer);
                                mediaView.getMediaPlayer().seek(Duration.ZERO);
                                updateSliderPosition(Duration.ZERO);
                                songSlider.setValue(0);
                                updateValues();
                                mediaPlayer.setVolume(volume);
                                mediaPlayer.play();
                                playIcon();
                            }
                        });
                        pauseSong();
                    }
                }
            });

            nextSongButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    seekAndUpdate(players.get(players.indexOf(mediaView.getMediaPlayer())).getTotalDuration());
                }
            });

            previousSongButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    seekAndUpdate(Duration.ZERO);
                }
            });



            songSlider.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    Bounds b1 = songSlider.getLayoutBounds();
                    double mouseX = event.getX();
                    double percent = (((b1.getMinX() + mouseX) * 100) / (b1.getMaxX() - b1.getMinX()));
                    songSlider.setValue((percent) / 100);
                    seekAndUpdate(new Duration(mediaView.getMediaPlayer().getTotalDuration().toMillis() * percent / 100));
                    songSlider.setValueFactory(slider ->
                            Bindings.createStringBinding(
                                    () -> (secToMin((long) mediaView.getMediaPlayer().getCurrentTime().toSeconds())),
                                    songSlider.valueProperty()
                            )
                    );
                }
            });

        }
        else {
            if(pauseButton.isVisible()) {
                if ((mediaPlayer != null) && (mediaView != null)) {
                    mediaPlayer = mediaView.getMediaPlayer();
                    mediaPlayer.stop();
                    mediaView = null;
                    mediaPlayer = null;
                }
                pauseIcon();
            }
            System.out.println("Song does not exist!");
        }
    }

    public void setMain(Main main) {
        this.main = main;
    }


    public String kbToMb(long length) {
        Long l = length;
        double d = l.doubleValue();
        DecimalFormat df = new DecimalFormat("#.00");
        String form = df.format((d/1024)/1024);
        return form + "Mb";
    }

    public String secToMin(long sec) {
        Long s = sec;
        String time = null;
        if((s%60) < 10) {
            time = s/60 + ":0" + s%60;
        }
        else {
            time = s/60 + ":" + s%60;
        }
        return time;
    }

    public MediaPlayer createPlayer(String url) {
        url.replace("\\", "/");
        final Media media = new Media(new File(url).toURI().toString());
        final MediaPlayer player = new MediaPlayer(media);
        System.out.println("+++++ " + url);
        return player;
    }

    public Media createMedia(String url) {
        url.replace("\\", "/");
        final Media media = new Media(new File(url).toURI().toString());
        return media;
    }

    public void playIcon() {
        playButton.setVisible(false);
        playButton.setDisable(true);
        pauseButton.setVisible(true);
        pauseButton.setDisable(false);
    }

    public void pauseIcon() {
        pauseButton.setVisible(false);
        pauseButton.setDisable(true);
        playButton.setVisible(true);
        playButton.setDisable(false);
    }

    public void setCurrentlyPlayer(MediaPlayer player) throws Exception {
        // Media source is a file: URI - convert it back to a File (handles %20 etc. properly)
        File file = new File(new java.net.URI(player.getMedia().getSource()));
        Tag tag = AudioFileIO.read(file).getTag();
        artistName.setText(tagOr(tag, FieldKey.ARTIST, "Unknown"));
        songName.setText(tagOr(tag, FieldKey.TITLE, stripExtension(file.getName())));
        albumName.setText(tagOr(tag, FieldKey.ALBUM, "-"));
        showAlbumArt(file);
    }

    // Formats JavaFX's MediaPlayer can actually play. jaudiotagger can read tags from more
    // (flac, ogg, wma, opus...), but JavaFX can't decode those, so they're not listed here.
    private static final Set<String> PLAYABLE_EXTENSIONS =
            new HashSet<>(Arrays.asList("mp3", "wav", "aif", "aiff", "m4a", "aac", "mp4"));

    static {
        // jaudiotagger logs a lot of INFO/WARNING noise to the console - quiet it down
        Logger.getLogger("org.jaudiotagger").setLevel(Level.SEVERE);
    }

    private static boolean isPlayable(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && PLAYABLE_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase());
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    // Returns the tag value, or the fallback if the file has no tag / the field is empty
    private static String tagOr(Tag tag, FieldKey key, String fallback) {
        if (tag == null) return fallback;
        String value = tag.getFirst(key);
        return (value == null || value.trim().isEmpty()) ? fallback : value;
    }

    public void takeCare() throws Exception {
        if(songTable.getSelectionModel().getSelectedItem() != null) {
            Song song = songTable.getSelectionModel().getSelectedItem();
            playPauseSong(song);
        }
        else {
            System.out.println("null");
        }
    }

    private void seekAndUpdate(Duration duration) {
        final MediaPlayer player = players.get(players.indexOf(mediaView.getMediaPlayer()));

        player.seek(duration);
    }

    private void updateValues() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            final MediaPlayer player = mediaView.getMediaPlayer();
                            if((player.getStatus() != Status.PAUSED) && (player.getStatus() != Status.STOPPED) && (player.getStatus() != Status.READY)) {
                                double tduration = player.getTotalDuration().toSeconds();
                                totalDuration.setText(secToMin((long) tduration));
                                currentDuration.setText(secToMin((long) player.getCurrentTime().toSeconds()));
                                updateSliderPosition(player.getCurrentTime());
                                volumeHandler();
                            }
                        }
                    });
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
                while(!players.isEmpty());
            }
        });
        thread.start();
    }

    private void updateSliderPosition(Duration currentTime) {
        final MediaPlayer player = mediaView.getMediaPlayer();
        final Duration totalDuration = player.getTotalDuration();
        if((totalDuration == null) || (currentTime == null)) {
            songSlider.setValue(0);
        }
        else {
            songSlider.setValue((currentTime.toMillis() / totalDuration.toMillis()) * 100);
        }
    }

    private void volumeHandler() {
        volumeSlider.valueProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                mediaView.getMediaPlayer().setVolume(volumeSlider.getValue() / 100);
                volumeValue.setText(String.valueOf((int)volumeSlider.getValue()));
                volume = mediaView.getMediaPlayer().getVolume();
                volumeIconChanger();
            }
        });
    }

    private void volumeIconChanger() {
        if(volumeSlider.getValue() == 0) {
            muteIcon.setVisible(true);
            volumeIcon.setVisible(false);
        }
        else {
            muteIcon.setVisible(false);
            volumeIcon.setVisible(true);
        }
    }

    private void transitionOperation(AnchorPane anchorPane, FadeTransition fadeTransition, boolean isShowing) {
    	fadeTransition.setNode(anchorPane);
        fadeTransition.setDuration(Duration.millis(1000));
        fadeTransition.setFromValue(isShowing ? 0.0 : 1.0);
        fadeTransition.setToValue(isShowing ? 1.0 : 0.0);
        anchorPane.setVisible(isShowing);
        fadeTransition.play();
    }

    private void showTransition(AnchorPane anchorPane) {
        transitionOperation(anchorPane, fadeIn, true);
    }

    private void hideTransition(AnchorPane anchorPane) {
        transitionOperation(anchorPane, fadeOut, false);
    }

    // ---------------------------------------------------------------- Album art

    private static final int ALBUM_ART_SIZE = 300;        // decode size in px (pane is ~105px; extra is for HiDPI)
    private static final int ALBUM_ART_CACHE_SIZE = 100;  // max covers kept in memory
    // Image files looked for next to the song when it has no embedded cover, in priority order
    private static final List<String> FOLDER_ART_NAMES = Arrays.asList("cover", "folder", "front", "album");
    private static final List<String> FOLDER_ART_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");

    // Puts the ImageView into imagePane, scaled to fit and kept centred
    private void setUpAlbumArtView() {
        placeholderArt = new Image(ClassLoader.getSystemResource("images/Question.PNG").toExternalForm());
        albumArtView.setPreserveRatio(true);
        albumArtView.setSmooth(true);
        albumArtView.fitWidthProperty().bind(imagePane.widthProperty());
        albumArtView.fitHeightProperty().bind(imagePane.heightProperty());
        // A plain Pane doesn't centre its children, so re-centre whenever either size changes
        InvalidationListener centre = observable -> centreAlbumArt();
        albumArtView.layoutBoundsProperty().addListener(centre);
        imagePane.widthProperty().addListener(centre);
        imagePane.heightProperty().addListener(centre);
        imagePane.getChildren().add(albumArtView);
        showAlbumArt(null);
    }

    private void centreAlbumArt() {
        Bounds b = albumArtView.getLayoutBounds();
        albumArtView.relocate((imagePane.getWidth() - b.getWidth()) / 2,
                              (imagePane.getHeight() - b.getHeight()) / 2);
    }

    // Shows the cover for this audio file, or the placeholder if it has none (or file is null)
    private void showAlbumArt(File audioFile) {
        Image art = (audioFile == null) ? null : loadAlbumArt(audioFile);
        albumArtView.setImage(art != null ? art : placeholderArt);
    }

    // Embedded cover first, then a cover/folder image in the same folder. Results are cached.
    private Image loadAlbumArt(File audioFile) {
        String key = audioFile.getAbsolutePath();
        if (albumArtCache.containsKey(key)) {
            return albumArtCache.get(key);
        }
        Image art = readEmbeddedArt(audioFile);
        if (art == null) {
            art = loadFolderArt(audioFile.getParentFile());
        }
        albumArtCache.put(key, art);
        return art;
    }

    // Cached per folder, so every song in an album shares one decoded image
    private Image loadFolderArt(File folder) {
        if (folder == null) return null;
        String key = "folder:" + folder.getAbsolutePath();
        if (albumArtCache.containsKey(key)) {
            return albumArtCache.get(key);
        }
        Image art = readFolderArt(folder);
        albumArtCache.put(key, art);
        return art;
    }

    private static Image readEmbeddedArt(File audioFile) {
        try {
            Tag tag = AudioFileIO.read(audioFile).getTag();
            if (tag == null) return null;
            Artwork artwork = tag.getFirstArtwork();
            if (artwork == null || artwork.getBinaryData() == null) return null; // none, or only a URL link
            return decodeArt(new ByteArrayInputStream(artwork.getBinaryData()));
        }
        catch (Exception e) { // unreadable file, or a format whose tag doesn't support artwork
            return null;
        }
    }

    private static Image readFolderArt(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return null;
        for (String name : FOLDER_ART_NAMES) {
            for (String extension : FOLDER_ART_EXTENSIONS) {
                String wanted = name + "." + extension;
                for (File file : files) {
                    if (file.isFile() && file.getName().equalsIgnoreCase(wanted)) {
                        try (InputStream in = new FileInputStream(file)) {
                            Image art = decodeArt(in);
                            if (art != null) return art;
                        }
                        catch (IOException e) { /* unreadable - try the next candidate */ }
                    }
                }
            }
        }
        return null;
    }

    // Decodes straight to ALBUM_ART_SIZE so large embedded covers don't sit in memory full size.
    // Returns null if the bytes aren't a valid image.
    private static Image decodeArt(InputStream in) {
        Image image = new Image(in, ALBUM_ART_SIZE, ALBUM_ART_SIZE, true, true);
        return image.isError() ? null : image;
    }

    private void repeatSongs(){
        mediaView.getMediaPlayer().setOnRepeat(new Runnable() {
            @Override
            public void run() {
                mediaView.getMediaPlayer().seek(Duration.ZERO);
            }
        });
        if(isAutoplay) {
            mediaView.getMediaPlayer().play();
            playIcon();
        }
        else return;
    }

    private void pauseSong() {
        mediaView.getMediaPlayer().setAutoPlay(true);
        pauseButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (mediaView.getMediaPlayer().getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaView.getMediaPlayer().pause();
                    pauseButton.setVisible(false);
                    pauseButton.setDisable(true);
                    playButton.setVisible(true);
                    playButton.setDisable(false);
                }
            }
        });
    }
}

