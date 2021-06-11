//AWT graphics
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

//BufferedImage
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.*;

//SWING graphics
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

//Util packages
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.*;

//keyboard pause
import java.io.Console;

//serialization
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.Serializable;

public class applet {

    public static void main(String[] args) {
        new applet();
    }

    public class TrainPath extends Path2D.Double {

        public TrainPath(int size,int delta) {
            int radius = size;

            int centerX = size/2 ; //was size /2
            int centerY = size/2 ; //was size /2
            moveTo(centerX+delta, centerY+delta);
            append(new Ellipse2D.Double(centerX - radius, centerY - radius, 4.0 * radius, 2.0 * radius), true);

        }

    }


    public applet() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException |
                         IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }

                JFrame frame = new JFrame("Two Train Circuit Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new TestPane());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    public class TestPane extends JPanel {

        //Simulation parameters
        private Boolean AttackCase=false;
        private Boolean BruteAttack=true;
        private Boolean PlotResults=false;
        protected static final double PLAY_TIME = 90000; // higher, slower; lower, faster.


        private TrainPath trainPath1, trainPath2;
        private Shape pathShape, pathShape2;

        private final Rectangle box;

        private List<Point2D> pointsTrain1, pointsTrain2;
        private List<Double> distancePlot;
        private List<Integer> timePlot;
        private double angleTrain1, angleTrain2;
        private Point2D posTrain1,posTrain2;
        private int indexTrain1, indexTrain2;
        private int loopCounter1,loopCounter2,timeCounter;
        private int loop;


        private Long startTime;



        public TestPane() {

            Boolean noCollision=true;
            int maxLoops=15;
            timeCounter=0;
            loopCounter1=0;
            loopCounter2=0;
            box = new Rectangle(0, 0, 50, 50);//Train has a rectangular shape

                double w=800, x=100, y=200, h=300;
                double kappa = 0.5522848;
                double ox = (w / 2) * kappa; // control point offset horizontal
                double oy = (h / 2) * kappa; // control point offset vertical
                double xe = x + w;           // x-end
                double ye = y + h;           // y-end
                double xm = x + w / 2;       // x-middle
                double ym = y + h / 2;


                Path2D path = new Path2D.Double();
                Path2D path2 = new Path2D.Double();
                path.moveTo(x, ym);


                path.curveTo(x, ym - oy, xm - ox, y, xm, y);
                path.append(new Arc2D.Double(xm, y, 100, 301, 90, -180, Arc2D.OPEN), true);
                path.curveTo(xm - ox, ye, x, ym + oy, x, ym);

                path2.moveTo(x, ym);

                path2.curveTo(x, ym - oy, xm - ox, y, xm, y);
                path2.curveTo(xm + ox, y, xe, ym - oy, xe, ym);
                path2.curveTo(xe, ym + oy, xm + ox, ye, xm, ye);
                path2.curveTo(xm - ox, ye, x, ym + oy, x, ym);

                pathShape = path;
                pathShape2 = path2;

                //read pennylane traces
                double[] train1X;
                double[] train1Y;
                double[] train2X;
                double[] train2Y;

                try {

                    FileInputStream fis = new FileInputStream("data/TakeLoopX.dat");
                    //FileInputStream fis = new FileInputStream("data/train1X.dat");
                    ObjectInputStream iis = new ObjectInputStream(fis);
                    train1X = (double[]) iis.readObject();

                    fis = new FileInputStream("data/TakeLoopY.dat");
                    //fis = new FileInputStream("data/train1Y.dat");
                    iis = new ObjectInputStream(fis);
                    train1Y = (double[]) iis.readObject();

                    fis = new FileInputStream("data/TakeBypassX.dat");
                    //fis = new FileInputStream("data/train2X.dat");
                    iis = new ObjectInputStream(fis);
                    train2X = (double[]) iis.readObject();

                    fis = new FileInputStream("data/TakeBypassY.dat");
                    //fis = new FileInputStream("data/train2Y.dat");
                    iis = new ObjectInputStream(fis);
                    train2Y = (double[]) iis.readObject();



                    //DEL UNNECESSARY STAFF
                    trainPath1 = new TrainPath(100,0);
                    trainPath2 = new TrainPath(100,0);

                    pointsTrain1 = new ArrayList<>();
                    pointsTrain2 = new ArrayList<>();
                    timePlot = new ArrayList<>();
                    distancePlot = new ArrayList<>();
                    PathIterator piTrain1;
                    PathIterator piTrain2;

                    //10 loops
                    for (int j = 0; j < 10; j++){
                        for (int i = 0; i < train1X.length; i++){
                            pointsTrain1.add(new Point2D.Double(train1X[i], train1Y[i]));
                        }
                    }//10 loops

                    //10 loops
                    for (int j = 0; j < 10; j++){
                        for (int i = 0; i < train2X.length; i++){
                            pointsTrain2.add(new Point2D.Double(train2X[i], train2Y[i]));
                        }
                    }

                    posTrain1 = pointsTrain1.get(0);
                    posTrain2 = pointsTrain2.get(0);
                    Timer timer = new Timer(10, new ActionListener() {
                            @Override
                                public void actionPerformed(ActionEvent e) {

                                if (startTime == null) {
                                    startTime = System.currentTimeMillis();
                                }
                                long playTime = System.currentTimeMillis() - startTime;
                                double progress = playTime / PLAY_TIME;
                                if (progress >= 1.0) {
                                    progress = 1d;
                                    ((Timer) e.getSource()).stop();

                                    double[] x;
                                    double[] y;
                                    x = new double[timePlot.size()];
                                    y = new double[distancePlot.size()];

                                    for (int i=0; i < timePlot.size(); i++){
                                        x[i]=timePlot.get(i);
                                    }

                                    for (int i=0; i < distancePlot.size(); i++){
                                        y[i]=distancePlot.get(i);
                                    }

                                }

                                int indexTrain1 = Math.min(Math.max(0, (int) (pointsTrain1.size() * progress)), pointsTrain1.size() - 1);
                                int indexTrain2 = Math.min(Math.max(0, (int) (pointsTrain2.size() * progress)), pointsTrain2.size() - 1);

                                posTrain1 = pointsTrain1.get(indexTrain1);
                                if (indexTrain1 < pointsTrain1.size() - 1) {
                                    angleTrain1 = angleTo(posTrain1, pointsTrain1.get(indexTrain1 + 1));
                                }

                                posTrain2 = pointsTrain2.get(indexTrain2);
                                if (indexTrain2 < pointsTrain2.size() - 1) {
                                    angleTrain2 = angleTo(posTrain2, pointsTrain2.get(indexTrain2 + 1));
                                }
                                timeCounter++;
                                double distance = Point2D.distance(posTrain1.getX(),posTrain1.getY(),posTrain2.getX(),posTrain2.getY());

                                timePlot.add(timeCounter);
                                distancePlot.add(distance);

                                repaint();

                                //Stop the simulation when the distance between trains is close to 0, except at T=0
                                if ( (timeCounter>10) && (distance<1.5) ){
                                    progress = 1d;
                                    ((Timer) e.getSource()).stop();
                                }

                            }
                        });

                    timer.start();

                }catch (Exception e) {
                    System.out.print("Exception storage data!");
                }


        }

        protected double angleTo(Point2D from, Point2D to) {
            double angle = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
            return angle;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(1024, 780);//main window
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            setBackground(Color.white);
            Graphics2D g2d = (Graphics2D) g.create();
            BufferedImage iconTrain;

            Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
            g2d.setStroke(dashed);
            g2d.setColor(Color.GRAY);
            g2d.draw(pathShape);

            g2d.setStroke(new BasicStroke(6f));
            g2d.setColor(Color.BLACK);
            g2d.draw(pathShape2);


            int xT1 = (getWidth() - pathShape.getBounds().width) / 2;
            int yT1 = (getHeight() - pathShape.getBounds().height) / 2;

            int xT2 = (getWidth() - pathShape2.getBounds().width) / 2;
            int yT2 = (getHeight() - pathShape2.getBounds().height) / 2;


            g2d.translate(xT1, yT1);
            String str = "";
            double distance = Point2D.distance(posTrain1.getX(),posTrain1.getY(),posTrain2.getX(),posTrain2.getY());
            distance = Math. round(distance * 100.0) / 100.0;
            Font font = new Font("Times New Roman", Font.PLAIN, 18);
            g2d.setFont(font);

            if(AttackCase){
                if(BruteAttack){
                    g2d.drawString("[Agressive Attack Mode]", 1, 75);
                }else{
                    g2d.drawString("[Stealthy Attack Mode]", 1, 75);
                }
            }else{
                g2d.drawString("[Attack Mode, DL disabled]", 1, 75);
            }

            font = new Font("Times New Roman", Font.ITALIC, 24);
            g2d.setFont(font);

            str = "time=" + timeCounter;
            g2d.drawString(str, 1, 100);

            if(distance>3){
                str = "distance=" + distance;
                g2d.drawString(str, 1, 125);
            }else{
                str = "distance= 0.00";
                g2d.drawString(str, 1, 125);
                g2d.setColor(Color.RED);
                g2d.drawString("BOOM!!!", 1, 150);
            }

            AffineTransform at = new AffineTransform();

            g2d = (Graphics2D) g.create();

            if (posTrain1 != null) {
                Rectangle bounds = box.getBounds();
                at.rotate(angleTrain1, (bounds.width / 2), (bounds.width / 2));

                Path2D player = new Path2D.Double(box, at);

                g2d.translate(posTrain1.getX() - (bounds.width / 2), posTrain1.getY() - (bounds.height / 2));
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(6f));
                g2d.draw(player);

                try{
                    iconTrain = ImageIO.read(getClass().getResource("./FIG/red.png"));
                    g2d = (Graphics2D) g.create();

                    g2d = (Graphics2D) g.create();
                    g2d.drawImage(iconTrain, (int) posTrain1.getX()-(iconTrain.getWidth()/2), (int) posTrain1.getY()  - (iconTrain.getHeight()/2), this);
                }
                catch(Exception e){
                    e.printStackTrace();
                }

            }


            g2d = (Graphics2D) g.create();

            if (posTrain2 != null) {

                Rectangle bounds = box.getBounds();
                at.rotate(angleTrain2, (bounds.width / 2), (bounds.width / 2));

                Path2D player = new Path2D.Double(box, at);

                g2d.translate(posTrain2.getX() - (bounds.width / 2), posTrain2.getY() - (bounds.height / 2));
                g2d.setColor(Color.BLUE);
                g2d.setStroke(new BasicStroke(6f));
                g2d.draw(player);

                try{
                    iconTrain = ImageIO.read(getClass().getResource("./FIG/blue.png"));
                    g2d = (Graphics2D) g.create();

                    at = new AffineTransform();
                    at.rotate(angleTrain2, (iconTrain.getWidth() / 2), (iconTrain.getWidth() / 2));
                    at.translate(posTrain2.getX() - (iconTrain.getWidth() / 2), posTrain2.getY()- (iconTrain.getHeight() / 2));

                    g2d = (Graphics2D) g.create();
                    g2d.drawImage(iconTrain, (int) posTrain2.getX()-(iconTrain.getWidth()/2), (int) posTrain2.getY()  - (iconTrain.getHeight()/2), this);
                }
                catch(Exception e){
                    e.printStackTrace();
                }

            }

        }

    }

}
