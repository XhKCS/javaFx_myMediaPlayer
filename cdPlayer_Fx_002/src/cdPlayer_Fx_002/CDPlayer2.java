package cdPlayer_Fx_002;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

//import javax.print.DocFlavor.URL; //看来这个类不是我们需要用到的那个URL

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.Track;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class CDPlayer2 extends Application{
	private ArrayList<Song> songs = new ArrayList<Song>();
//	private ArrayList<Media> mediaList = new ArrayList<Media>();
//	private ArrayList<Image> imageList = new ArrayList<Image>();
	private MediaPlayer mP;
	private ImageView iV; //歌曲图片视图框
	private RotateTransition rT; //旋转动画
	
	private int index;
	private Song currentSong;
	private Map<String, String> currentLyrics; //当前的歌词
	
	private Slider s1; //设置为音量控制条
	private Slider s2; //想做歌曲进度控制条
	private boolean mousePressing = false;
	
	private Label timeLabel1;
	private Label timeLabel2;
	
	private Label lyricsLabel; //显示歌词
	
	private AnchorPane pane;
	private Scene scene;
	
	private ImageView playBtn; //播放键与暂停键组合，用ImageView代替Button
	private Image startIM;
	private Image pauseIM;
	private boolean isPaused = true;
	
	private VBox playList;
	//private TranslateTransition tt;
	//private boolean isVisible = false;
	private ArrayList<Label> songNameList = new ArrayList<Label>(); //歌曲名的label集合
	
	//加入花瓣飘舞动画
	private ArrayList<ParallelTransition> flowerTransition_list = new ArrayList<ParallelTransition>();
	
	public void setXY(Node node, int X, int Y) {
		node.setLayoutX(X);
		node.setLayoutY(Y);
	}
	
	public void iVSetWH(ImageView iv, int W, int H) {
		iv.setFitWidth(W);
		iv.setFitHeight(H);
	}
	
	public static Color getRandomColor() {
		Random random = new Random();
		int r = random.nextInt(256);
		int g = random.nextInt(256);
		int b = random.nextInt(256);
		
		return Color.rgb(r, g, b);
	}
	
	public void updatePlay() {
		if (index > songs.size()-1) {
			index = 0;
		}
		else if (index < 0) {
			index = songs.size()-1;
		}
		currentSong = songs.get(index);
		currentLyrics = currentSong.getLyrics();
		
		if (rT != null) {
			rT.setFromAngle(0); //回到起始旋转角度：0  好像没啥作用，为什么呢？
			rT.pause();
		}
		
		Image im = currentSong.getImage();
		if (iV != null) {
			iV.setImage(im);
			//iV已存在，就不要重复添加到根节点了
		}
		else {
			iV = new ImageView(im);
			pane.getChildren().add(iV);
		}
		
		lyricsLabel.setText(""); //先将之前的歌词清空
		
		for (Label k : songNameList) {
			if (k.getText().equals(currentSong.getName())) {
				k.setTextFill(getRandomColor());
			}
		}
		
		Media m = currentSong.getMedia();
		if (mP != null) {
			mP.dispose();
		}
		mP = new MediaPlayer(m); //做这一步之前mP要么还没创建，要么已经dispose()了
		
		//得先加载好之后再与滑动条绑定!
		mP.setOnReady(new Runnable() {
			@Override
			public void run() {
				mP.volumeProperty().bind(s1.valueProperty()); //音量值与滑动条的设定值绑定
				
				//进度条的操作
				s2.setValue(0); //从0开始
				s2.setMin(0);
				s2.setMax(mP.getTotalDuration().toSeconds()); //设置最小值与最大值
				
				timeLabel2.setText(String.format("%02d:%02d", (int)(mP.getTotalDuration().toMinutes()), (int)(mP.getTotalDuration().toSeconds()%60) ));
				
				if (currentLyrics == null) {
					lyricsLabel.setText("当前歌曲暂时没有获取到歌词哦~");
				}
				//监听歌曲当前播放的时间，很多操作都要在该监听方法中做
				mP.currentTimeProperty().addListener(new ChangeListener<Duration>() {
					// 监听播放器的当前时间进度
					@Override
					public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
						if (mousePressing == false) {
							s2.setValue(newValue.toSeconds()); //让进度条的当前值与歌曲进度当前值一致（单向绑定）
						}
						//String.format设置指定格式的字符串
						timeLabel1.setText(String.format("%02d:%02d", (int)(mP.getCurrentTime().toMinutes() ), (int)(mP.getCurrentTime().toSeconds()%60) ));
						
						if (currentLyrics != null) {
							//通过当前时间获取到对应那一句歌词
							String line = currentLyrics.get(timeLabel1.getText());
							if (line != null) {
								lyricsLabel.setText(line);
							}
						}
						
						//下面的好像没用，难道是歌曲结束后MediaPlayer已经自动注销了？
//						if (newValue.toSeconds() == mP.getTotalDuration().toSeconds()) {
//							++index;
//							updatePlay();
//							play();
//						}
						
						
					}
				});
				
				System.out.println("\n");
				//下面再进行一些打印音频信息的操作
				for (Track k : m.getTracks()) 
				{
					System.out.println(k);
				}
				
				System.out.println("==============分割线============");
				
				ObservableMap<String, Object> map = m.getMetadata();
				for (String key : map.keySet()) 
				{
					System.out.println(key +"--"+ map.get(key)); //打印歌曲信息键值对，可能还包括专辑图片地址
				}
					
				mP.setAudioSpectrumListener(new AudioSpectrumListener() {
					// 可获取歌词？
					@Override
					public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
						//System.out.println(magnitudes[0]); //音乐幅度
						//System.out.println(phases[1]);
					}
					
				});
				
				
			}	
		});
		
		
	}
	
	// 下面两个是播放/暂停键要做的操作单独封装成方法
	public void play() {
		mP.play();
		rT.play();
		playBtn.setImage(pauseIM);
		isPaused = false;
	}
	
	public void pause() {
		mP.pause();
		rT.pause();
		playBtn.setImage(startIM);
		isPaused = true;
	}
	
	
	public void initSongs() throws MalformedURLException {
		//还是绝对路径最好使，最不容易出问题
		java.net.URL mediaUrl1 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/songs/Sincerely.mp3");
		java.net.URL mediaUrl2 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/songs/02.mp3");
		java.net.URL mediaUrl3 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/songs/Cagayake!GIRLS.mp3");
		java.net.URL mediaUrl4 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/songs/渚.mp3");
		java.net.URL mediaUrl5 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/songs/小小的手心.mp3");
		URL imageUrl1 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/images/封面1.png");
		URL imageUrl2 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/images/封面2.jpg");
		URL imageUrl3 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/images/封面3.jpg");
		URL imageUrl4 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/images/封面4.jpg");
		
		Song s1 = new Song("Sincerely", mediaUrl1, imageUrl1);
		Song s2 = new Song("02", mediaUrl2, imageUrl2);
		Song s3 = new Song("Cagayake!GIRLS", mediaUrl3, imageUrl3);
		Song s4 = new Song("渚", mediaUrl4, imageUrl4);
		Song s5 = new Song("小小的手心", mediaUrl5, imageUrl4);
		
		songs.add(s1);
		songs.add(s2);
		songs.add(s3);
		songs.add(s4);
		songs.add(s5);
		
		//Image im3 = (Image)(m3.getMetadata().get("image")); 
		//试了一下发现并不能获取到专辑图片？为什么呢？
		
	
	}
	
	//下面是对一些按钮的初始化封装成方法
	public void initButtons() throws MalformedURLException {
		//  播放键与暂停键组合
        java.net.URL url1 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/images/播放键.jpg");
		startIM = new Image(url1.toExternalForm());
		java.net.URL url2 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/images/暂停键.jpg");
		pauseIM = new Image(url2.toExternalForm());
		
		playBtn = new ImageView();
		iVSetWH(playBtn, 45, 45);
		setXY(playBtn, 505, 795);
		
		isPaused = true;
		playBtn.setImage(startIM);
		playBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				if (isPaused == true) {
					play();
				}
				else {
					pause();
				}
				
			}
			
		});
		
		//  下一首按键
		java.net.URL url3 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/images/下一首键.jpg");
		ImageView nextSongIV = new ImageView(new Image(url3.toExternalForm()));
		iVSetWH(nextSongIV, 35, 35);
		setXY(nextSongIV, 570, 800);
		nextSongIV.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				//mP.dispose(); //将之前的MediaPlayer注销的操作也放在update方法里了
				rT.stop();
				++index;
				updatePlay();
				play();	
			}
			
		});
	
		//  上一首按键
		java.net.URL url4 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/images/上一首键.jpg");
		ImageView preSongIV = new ImageView(new Image(url4.toExternalForm()));
		iVSetWH(preSongIV, 35, 35);
		setXY(preSongIV, 450, 800);
		preSongIV.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				//mP.dispose(); 
				rT.stop();
				--index;
				updatePlay();
				play();
			}
			
		});
		
		pane.getChildren().addAll( playBtn, nextSongIV, preSongIV);
		
	}
	
	// 初始化播放列表
	public void initPlayList() throws MalformedURLException {
		playList = new VBox();
		playList.setPrefSize(350, 800);
		playList.setLayoutX(1050);
		playList.setLayoutY(0);
		playList.setStyle("-fx-background-color: linear-gradient(to bottom right, white, SkyBlue);");
		
		playList.setPadding(new Insets(10)); //内边距
		playList.setSpacing(30); //间距
		playList.setAlignment(Pos.TOP_CENTER); //对齐方式
		
		Label mainLabel = new Label("当前播放列表");
		VBox.setMargin(mainLabel, new Insets(20));
		mainLabel.setFont(Font.font(22));
		mainLabel.setTextFill(Color.valueOf("#FF60AF")); 
		playList.getChildren().add(mainLabel);
		
		for (Song k : songs) 
		{
			Label label = new Label(k.getName());
			//label.setPrefSize(300, 40);
			label.setFont(Font.font(18));
			label.setTextFill(getRandomColor()); //字体为随机颜色
			label.setOnMouseClicked(new EventHandler<MouseEvent>() 
			{
				@Override
				public void handle(MouseEvent arg0) {
					//可以做个被选中的效果
					
					for (int i=0; i<songs.size(); i++) {
						if (songs.get(i).getName().equals(label.getText())) {
							index = i;
							updatePlay();
							play();
							break;
						}
					}
					
				}
			});
			
			songNameList.add(label);
		}
		
		playList.getChildren().addAll(songNameList);
		
		playList.setVisible(false);
		//不知道为啥，位移动画无效，直接用setVisible算了
//		tt = new TranslateTransition(Duration.seconds(1), playList);
//		tt.setAutoReverse(true);
//		tt.setFromX(100);
//		tt.setToX(500);
//		tt.setCycleCount(Animation.INDEFINITE);
//		tt.setInterpolator(Interpolator.DISCRETE); //无动画效果，直接跳到目标位置
//		tt.play();
		
		java.net.URL url1 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/images/列表键2.jpg");
		ImageView listBtn = new ImageView(new Image(url1.toExternalForm()));
		iVSetWH(listBtn, 50, 50);
		setXY(listBtn, 1300, 820);
		listBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				//控制位移动画方向 / 控制是否可见
				if (playList.isVisible() == false) {

					playList.setVisible(true);

				}
				else {

					playList.setVisible(false);
				}
				
			}
		});
		
		//最后别忘记加到根节点上
		pane.getChildren().addAll(playList, listBtn);
		
	}
	
	@Override
	public void start(Stage arg0) throws Exception {
		Stage stage = new Stage();
		stage.setTitle("多歌曲切换CD-Player_试验版");
		
		pane = new AnchorPane();
		scene = new Scene(pane, 1400, 900);
		
		stage.setScene(scene);
		stage.centerOnScreen();
		stage.setResizable(false);
		
		//stage.initStyle(StageStyle.TRANSPARENT); //透明
		scene.setFill(Color.TRANSPARENT); //透明
		pane.setBackground(new Background(new BackgroundFill(Color.LIGHTPINK, new CornerRadii(25), null))); //CornerRadii为设置圆角
		//设置渐变色
		pane.setStyle("-fx-background-color: linear-gradient(to bottom right, pink, white, LightBlue);");		
		
		pane.setOpacity(0.8); //设置不透明度
		
		//  flowerView估计要置于最底层，这样才不影响其他按钮啥的
		Node flowerView = getFlowerView(32, (int)scene.getWidth(), (int)scene.getHeight(), 2700);
		pane.getChildren().addAll(flowerView);
		
		//  音量条
		s1 = new Slider(0, 1, 0.2);
		//  将滑动条设置为竖直方向
		s1.setOrientation(javafx.geometry.Orientation.VERTICAL); 
		setXY(s1, 662, 700);
		s1.setPrefHeight(100);
		s1.setVisible(false);
		
		Label l1 = new Label((int)(s1.getValue()*100) +"%");
		setXY(l1, 657, 673);
		l1.setPrefSize(40, 30);
		l1.setStyle("-fx-font-size: 14px;");
		l1.setVisible(false);
		
		// 创建图像视图并设置图像
		java.net.URL url1 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/images/音量键.jpg");
		Image volumeIM1 = new Image(url1.toExternalForm());
		
		java.net.URL url2 = new URL("file:///D:/javaCode/eclipseCode/cdPlayer_Fx_002/resources/images/静音键.jpg");
		Image volumeIM2 = new Image(url2.toExternalForm());
		
        ImageView volumeIV = new ImageView(volumeIM1); 
        // 设置图像视图的大小
        iVSetWH(volumeIV, 37, 37);
        setXY(volumeIV, 650, 800);
//        // 将图像视图设置为按钮的图标
//        volumeBtn.setGraphic(volumeIV);
		
		//监听音量条的值并让label更新显示
		s1.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				l1.setText((int)(s1.getValue()*100) +"%");
				if (newValue.doubleValue() == 0) {
					volumeIV.setImage(volumeIM2);
				}
				else {
					volumeIV.setImage(volumeIM1);
				}
			}
		});
		// 直接用ImageView当做按钮
		volumeIV.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				if (s1.isVisible() == true) {
					s1.setVisible(false);
					l1.setVisible(false);
				}
				else {
					s1.setVisible(true);
					l1.setVisible(true);
				}
				
			}
			
		});
		
		//下面为音量按钮设置鼠标放上或移开的监听器
		volumeIV.hoverProperty().addListener(new ChangeListener<Boolean>( ) {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue.booleanValue() == true) {
					//鼠标正放在按钮上
					s1.setVisible(true);
					l1.setVisible(true);
				}
//				else {
//					s1.setVisible(false);
//				}
				
			}
		});
		//音量条自己也要加上该监听器
		s1.hoverProperty().addListener(new ChangeListener<Boolean>( ) {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue.booleanValue() == true) {
					//鼠标正放在音量条上
					s1.setVisible(true);
					l1.setVisible(true);
				}
				else {
					s1.setVisible(false);
					l1.setVisible(false);
				}
			}
			
		});
		
		//  进度条
		s2 = new Slider(0, 1, 0.5); //为了能滑动，new的时候得先给个值
		s2.setPrefWidth(500);
		setXY(s2, 360, 850);
		
		Label l2 = new Label("歌曲进度");
		l2.setStyle("-fx-font-size: 15px;");
		setXY(l2, 235, 837);
		l2.setPrefSize(70, 40);
		// 左端：当前时长
		timeLabel1 = new Label("00:00");
		timeLabel1.setStyle("-fx-font-size: 15px;");
		setXY(timeLabel1, 310, 837);
		timeLabel1.setPrefSize(70, 40); //在update方法中mP加载好后会监听其时间并改变该Label的值
		// 右端：歌曲总时长
		timeLabel2 = new Label("00:00");
		timeLabel2.setStyle("-fx-font-size: 15px;");
		setXY(timeLabel2, 870, 837);
		timeLabel2.setPrefSize(70, 40); //在update方法中mP加载好后会设置该Label的值
		
		//下面进行进度条的实现操作
		s2.setOnMousePressed(event -> {
			mousePressing = true; //确保在鼠标拖动滑动条的过程中滑动条的值不再随音乐进度改变
		});		
		//松开鼠标说明已经完成了鼠标拖动的操作，下面就应该让音乐进度马上跳转到当前滑动条被拖动到的值
		s2.setOnMouseReleased(event -> {
			//注意
			mP.seek(Duration.seconds(s2.getValue())); //将音频进度跳转到指定位置
			mousePressing = false;
			
		});
		//下面是让歌曲能自动播放下一首
		s2.valueProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if (newValue.doubleValue() == s2.getMax()) {
					++index;
					updatePlay();
					play();
				}
				
			}
			
		});
		
		//下面初始化一下歌词Label
		lyricsLabel = new Label();
		lyricsLabel.setFont(Font.font(22));
		lyricsLabel.setTextFill(Color.valueOf("#33a3dc"));
		setXY(lyricsLabel, 650, 270);
		lyricsLabel.setPrefSize(400, 150);
		
		
		pane.getChildren().addAll(s1, volumeIV, s2, l1, l2, timeLabel1, timeLabel2, lyricsLabel);
		
		
		initSongs(); //加载歌曲封装成方法
		//至此，资源都加载完毕，之后传参只需要数字（歌曲标号）；若把index设为成员变量则无需额外传参
		index = 0;
		updatePlay();
		
		//在updatePlay之后iV才不为空
		// 创建圆形剪辑
        Circle circle = new Circle();
        circle.radiusProperty().bind(iV.fitWidthProperty().divide(2));
        circle.centerXProperty().bind(iV.fitWidthProperty().divide(2));
        circle.centerYProperty().bind(iV.fitHeightProperty().divide(2));
        //以上操作是将圆的属性与imageView的属性绑定

        iV.setClip(circle); //设置裁剪区域的模具，先要将裁剪区域绑定到ImageView的尺寸上，使其能跟随调整大小
      
   	 // 再设置ImageView的自适应属性
       iV.setPreserveRatio(true);
       iV.setLayoutX(100);
       iV.setLayoutY(100); //可以控制imageView的左右位置在整个窗口的中央
       iV.fitWidthProperty().bind(scene.widthProperty().divide(1.6 * 1.25 * 1.4));
       iV.fitHeightProperty().bind(scene.heightProperty().divide(1.6 * 1.125)); //根据 窗口大小合理设置，保证imageView是正方形
        
        // 创建旋转动画
        rT = new RotateTransition(Duration.seconds(18), iV); //一个周期为18秒
        rT.setByAngle(360);
        rT.setCycleCount(Animation.INDEFINITE); //循环次数
        rT.setAutoReverse(false); 
        rT.setInterpolator(Interpolator.LINEAR); //这一行是什么？
		
        //以上操作只需做一次，每次更新只要改变iV的Image，以及mP
        
		//加载好其他按钮
        initButtons();
		
        //播放列表
		initPlayList();
        
//		HostServices host = this.getHostServices();
//		host.showDocument("www.bilibili.com");  //程序运行到这里时会自动用默认浏览器打开该网页
		
		stage.show();
		
		flowerTransition_list.forEach(new Consumer<ParallelTransition>()
		{
			@Override
			public void accept(ParallelTransition t) {
				t.play();
				
			}
		});
		
	}
	
	public Node getFlowerView(int number, int w, int h, int z) 
	{
		//最终要返回的是一个SubScene（3D场景），也是scene的一种，需要它自己的根节点
		AnchorPane ap = new AnchorPane();
		ap.setStyle("-fx-background-color: #FFB6C100"); //无颜色
		
		SubScene subscene = new SubScene(ap, w, h, true, SceneAntialiasing.BALANCED);
		
		PerspectiveCamera camera = new PerspectiveCamera(); //这是用来干啥的呢？
		subscene.setCamera(camera);
		
		//下面对各花朵位置坐标进行随机化的处理
		ArrayList<ImageView> imgList = new ArrayList<ImageView>();
		
		Random random = new Random();
		int num1 = random.nextInt(number);
		int num2 = number - num1; //两种花的数量
		
		int location_x;
		int location_y;
		int location_z;
		//  第一种花
		for (int i = 0; i < num1; i++)
		{
			ImageView iv = new ImageView("file:///D:/javaCode/eclipseCode/flowerAnimation/resources/flowers/花3.png");
			iv.setPreserveRatio(true); //保持宽高比
			iv.setFitWidth(100);
			
			//  对x坐标向两边做个分散
			if (random.nextBoolean() == true) {
				location_x = random.nextInt(w) + random.nextInt(300) + 300;
			}
			else {
				location_x = random.nextInt(w) - random.nextInt(300) - 300;
			}
			location_y = random.nextInt(30);
			location_z = random.nextInt(z);
			//注意坐标不要设成别的了！是Translate
			iv.setTranslateX(location_x);
			iv.setTranslateY(location_y);
			iv.setTranslateZ(location_z);
			
			iv.setOpacity(0); //在动画开始前先让其不可见
			
			imgList.add(iv);
		}
		//  第二种花
		for (int i = 0; i < num2; i++)
		{
			ImageView iv = new ImageView("file:///D:/javaCode/eclipseCode/flowerAnimation/resources/flowers/花朵2.png");
			iv.setPreserveRatio(true); //保持宽高比
			iv.setFitWidth(100);
			
			//  对x坐标向两边做个分散
			if (random.nextBoolean() == true) {
				location_x = random.nextInt(w) + random.nextInt(300) + 300;
			}
			else {
				location_x = random.nextInt(w) - random.nextInt(300) - 300;
			}
			location_y = random.nextInt(30);
			location_z = random.nextInt(z);	
			//注意坐标不要设成别的了！是Translate
			iv.setTranslateX(location_x);
			iv.setTranslateY(location_y);
			iv.setTranslateZ(location_z); 
			
			iv.setOpacity(0); //在动画开始前先让其不可见
			
			imgList.add(iv);
		}
		
		ap.getChildren().addAll(imgList);
		
		//下面创建动画
		imgList.forEach(new Consumer<ImageView>()
		{
			@Override
			public void accept(ImageView k) {
				double time = random.nextDouble() * 8 + 5;
				
				//  首先是位移动画
				TranslateTransition tt = new TranslateTransition(Duration.seconds(time));		
				tt.setFromX(k.getTranslateX());
				tt.setFromY(k.getTranslateY()); //从原位置开始	
				tt.setByX(400); 
				tt.setByY(1700); //都往右下角飘，由于路程相同，时间不同，故速度不同
				
				FadeTransition ft1 = new FadeTransition(Duration.seconds(time / 2));
				ft1.setFromValue(0);
				ft1.setToValue(1); //从看不见到浮现
				
				FadeTransition ft2 = new FadeTransition(Duration.seconds(3));
				ft2.setFromValue(1);
				ft2.setToValue(0); //从看得见到消失
				
				// 这又是什么？
				SequentialTransition st = new SequentialTransition();
				st.getChildren().addAll(ft1, ft2);
				
				//  加入花朵旋转效果
				RotateTransition rt = new RotateTransition(Duration.seconds(time));
				rt.setFromAngle(0);
				rt.setToAngle(360);
				
				
				// 用并行动画，这样就能同时控制多个动画，其子动画就不需要再设置Node了
				ParallelTransition pt = new ParallelTransition(); 
				pt.setNode(k);
				pt.getChildren().addAll(tt, st, rt);
				
				pt.setCycleCount(Animation.INDEFINITE);
				//pt.play();
				flowerTransition_list.add(pt); //在外面由键盘快捷键控制动画播放
				
			}
			
		});
			
		
		return subscene;
	}
	
	
	public static void main(String[] args) {
		launch(args);

	}
	
	
	
}
