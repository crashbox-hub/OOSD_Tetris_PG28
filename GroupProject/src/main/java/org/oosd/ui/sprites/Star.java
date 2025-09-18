package org.oosd.ui.sprites;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

final class Star extends Polygon {
    Star(double radiusOuter, double radiusInner, Color fill, Color stroke) {
        // 5-point star
        int points = 5;
        double angle = Math.PI / points;
        for (int i = 0; i < points * 2; i++) {
            double r = (i % 2 == 0) ? radiusOuter : radiusInner;
            double a = i * angle - Math.PI / 2;
            getPoints().addAll(
                    r * Math.cos(a),
                    r * Math.sin(a)
            );
        }
        setFill(fill);
        setStroke(stroke);
        setStrokeWidth(1.2);
    }
}
