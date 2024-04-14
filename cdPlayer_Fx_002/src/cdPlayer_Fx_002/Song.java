package cdPlayer_Fx_002;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.media.Media;

public class Song {
	private String name;
	private Media media;
	private Image image;
	private Map<String, String> lyrics;
	
	
	public Song (String name, URL mediaUrl, URL imageUrl) {
		this.name = name;
		media = new Media(mediaUrl.toExternalForm());
		image = new Image(imageUrl.toExternalForm());
		lyrics = null;
	}
	
	public Song (String name, URL mediaUrl, URL imageUrl, String lyricPath) {
		this.name = name;
		media = new Media(mediaUrl.toExternalForm());
		image = new Image(imageUrl.toExternalForm());
		lyrics = loadLyrics(lyricPath);
	}
	
	public String getName() {
		return name;
	}

	public Map<String, String> loadLyrics(String lyricPath) {
		Map<String, String> ly = new HashMap<String, String>();
		//要获取歌词文件并解析，得到播放时间与歌词对应的Map
		try {
			BufferedReader reader = new BufferedReader(
										new InputStreamReader(new FileInputStream(lyricPath), "utf-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				lyrics.put(null, line);
			}
			
			reader.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		return ly;
	}
	

	public Media getMedia() {
		return media;
	}

	public Image getImage() {
		return image;
	}

	public Map<String, String> getLyrics() {
		return lyrics;
	}

	public static void main(String[] args) {
		
	}

}
