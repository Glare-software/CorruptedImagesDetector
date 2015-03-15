package software.glare.cid.ui;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

/**
 * thanks to http://stackoverflow.com/questions/26702519/javafx-line-curve-with-arrow-head
 */
public class CubicCurveWithArrows extends Group {

    public CubicCurveWithArrows(Node from, Node to, boolean bidirectional) {


        Point2D localFromMin = new Point2D(
                from.getBoundsInLocal().getMinX(),
                from.getBoundsInLocal().getMinY()
        );

        Point2D localFromMax = new Point2D(
                from.getBoundsInLocal().getMaxX(),
                from.getBoundsInLocal().getMaxY()
        );

        Point2D localToMin = new Point2D(
                to.getBoundsInLocal().getMinX(),
                to.getBoundsInLocal().getMinY()
        );

        Point2D localToMax = new Point2D(
                to.getBoundsInLocal().getMaxX(),
                to.getBoundsInLocal().getMaxY()
        );


        Point2D sceneFromMin = from.localToScene(localFromMin);
        Point2D sceneFromMax = from.localToScene(localFromMax);

        Point2D sceneToMin = to.localToScene(localToMin);
        Point2D sceneToMax = to.localToScene(localToMax);

        Point2D sceneFromAvg = getAvgPoint2D(sceneFromMin, sceneFromMax);
        Point2D sceneToAvg = getAvgPoint2D(sceneToMin, sceneToMax);
        CubicCurve curve = createCurve(
                sceneFromAvg,
                sceneToAvg
        );

        double[] arrowShape = new double[]{0, 0, 5, 15, -5, 15};
        //backShape is used for absolute positioning
        Rectangle backShape = new Rectangle(from.getScene().getWidth(), from.getScene().getHeight());

        /*backShape.widthProperty().bind(new DoubleBinding() {
            {
                super.bind(from.boundsInLocalProperty());
            }

            @Override
            protected double computeValue() {
                return 0;
            }
        });*/
        backShape.setStyle("-fx-fill: transparent");
        getChildren().addAll(backShape, curve, new Arrow(curve, 1f, arrowShape));
        if (bidirectional) {
            getChildren().add(new Arrow(curve, 0f, arrowShape));
        }
    }



    private static Point2D getAvgPoint2D(Point2D min, Point2D max) {
        return new Point2D(min.getX() + (max.getX() - min.getX()) / 2, min.getY() + (max.getY() - min.getY()) / 2);
    }


    private CubicCurve createCurve(Point2D from, Point2D to) {
        CubicCurve curve = new CubicCurve();
        curve.setStartX(from.getX());
        curve.setStartY(from.getY());
        curve.setControlX1(50);
        curve.setControlY1(50);
        curve.setControlX2(100);
        curve.setControlY2(100);
        curve.setEndX(to.getX());
        curve.setEndY(to.getY());
        curve.setStroke(Color.ORANGE);
        curve.setStrokeWidth(1);
        //curve.setStrokeLineCap(StrokeLineCap.ROUND);
        curve.setFill(Color.TRANSPARENT);
        return curve;
    }

    class Arrow extends Polygon {

        private float t;
        private CubicCurve curve;
        private Rotate rz;

        public Arrow(CubicCurve curve, float t, double... arrowShape) {
            super(arrowShape);
            this.curve = curve;
            this.t = t;
            init();
        }

        private void init() {

            setFill(Color.web("#ff0900"));

            rz = new Rotate();
            {
                rz.setAxis(Rotate.Z_AXIS);
            }
            getTransforms().addAll(rz);

            update();
        }

        public void update() {
            double size = Math.max(curve.getBoundsInLocal().getWidth(), curve.getBoundsInLocal().getHeight());
            double scale = size / 4d;

            Point2D ori = eval(curve, t);
            Point2D tan = evalDt(curve, t).normalize().multiply(scale);

            setTranslateX(ori.getX());
            setTranslateY(ori.getY());

            double angle = Math.atan2(tan.getY(), tan.getX());

            angle = Math.toDegrees(angle);

            // arrow origin is top => apply offset
            double offset = -90;
            if (t > 0.5)
                offset = +90;

            rz.setAngle(angle + offset);

        }

        /**
         * Evaluate the cubic curve at a parameter 0<=t<=1, returns a Point2D
         *
         * @param c the CubicCurve
         * @param t param between 0 and 1
         * @return a Point2D
         */
        private Point2D eval(CubicCurve c, float t) {
            Point2D p = new Point2D(Math.pow(1 - t, 3) * c.getStartX() +
                    3 * t * Math.pow(1 - t, 2) * c.getControlX1() +
                    3 * (1 - t) * t * t * c.getControlX2() +
                    Math.pow(t, 3) * c.getEndX(),
                    Math.pow(1 - t, 3) * c.getStartY() +
                            3 * t * Math.pow(1 - t, 2) * c.getControlY1() +
                            3 * (1 - t) * t * t * c.getControlY2() +
                            Math.pow(t, 3) * c.getEndY());
            return p;
        }

        /**
         * Evaluate the tangent of the cubic curve at a parameter 0<=t<=1, returns a Point2D
         *
         * @param c the CubicCurve
         * @param t param between 0 and 1
         * @return a Point2D
         */
        private Point2D evalDt(CubicCurve c, float t) {
            Point2D p = new Point2D(-3 * Math.pow(1 - t, 2) * c.getStartX() +
                    3 * (Math.pow(1 - t, 2) - 2 * t * (1 - t)) * c.getControlX1() +
                    3 * ((1 - t) * 2 * t - t * t) * c.getControlX2() +
                    3 * Math.pow(t, 2) * c.getEndX(),
                    -3 * Math.pow(1 - t, 2) * c.getStartY() +
                            3 * (Math.pow(1 - t, 2) - 2 * t * (1 - t)) * c.getControlY1() +
                            3 * ((1 - t) * 2 * t - t * t) * c.getControlY2() +
                            3 * Math.pow(t, 2) * c.getEndY());
            return p;
        }


    }



}

