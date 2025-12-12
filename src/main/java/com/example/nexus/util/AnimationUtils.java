package com.example.nexus.util;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

public class AnimationUtils {
    /**
     * Create a fade in animation
     */
    public static FadeTransition fadeIn(Node node, Duration duration) {
        FadeTransition fadeTransition = new FadeTransition(duration, node);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        return fadeTransition;
    }

    /**
     * Create a fade out animation
     */
    public static FadeTransition fadeOut(Node node, Duration duration) {
        FadeTransition fadeTransition = new FadeTransition(duration, node);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        return fadeTransition;
    }

    /**
     * Create a slide in animation from the top
     */
    public static Timeline slideInFromTop(Node node, Duration duration) {
        double startY = -node.getBoundsInParent().getHeight();
        node.setTranslateY(startY);

        Timeline timeline = new Timeline();
        KeyValue keyValue = new KeyValue(node.translateYProperty(), 0);
        KeyFrame keyFrame = new KeyFrame(duration, keyValue);
        timeline.getKeyFrames().add(keyFrame);

        return timeline;
    }

    /**
     * Create a slide in animation from the bottom
     */
    public static Timeline slideInFromBottom(Node node, Duration duration) {
        double endY = node.getLayoutY();
        double startY = endY + node.getBoundsInParent().getHeight();
        node.setTranslateY(startY);

        Timeline timeline = new Timeline();
        KeyValue keyValue = new KeyValue(node.translateYProperty(), endY);
        KeyFrame keyFrame = new KeyFrame(duration, keyValue);
        timeline.getKeyFrames().add(keyFrame);

        return timeline;
    }

    /**
     * Create a slide in animation from the left
     */
    public static Timeline slideInFromLeft(Node node, Duration duration) {
        double startX = -node.getBoundsInParent().getWidth();
        node.setTranslateX(startX);

        Timeline timeline = new Timeline();
        KeyValue keyValue = new KeyValue(node.translateXProperty(), 0);
        KeyFrame keyFrame = new KeyFrame(duration, keyValue);
        timeline.getKeyFrames().add(keyFrame);

        return timeline;
    }

    /**
     * Create a slide in animation from the right
     */
    public static Timeline slideInFromRight(Node node, Duration duration) {
        double endX = node.getLayoutX();
        double startX = endX + node.getBoundsInParent().getWidth();
        node.setTranslateX(startX);

        Timeline timeline = new Timeline();
        KeyValue keyValue = new KeyValue(node.translateXProperty(), endX);
        KeyFrame keyFrame = new KeyFrame(duration, keyValue);
        timeline.getKeyFrames().add(keyFrame);

        return timeline;
    }

    /**
     * Create a scale in animation
     */
    public static Timeline scaleIn(Node node, Duration duration) {
        node.setScaleX(0.0);
        node.setScaleY(0.0);

        Timeline timeline = new Timeline();
        KeyValue keyValueX = new KeyValue(node.scaleXProperty(), 1.0);
        KeyValue keyValueY = new KeyValue(node.scaleYProperty(), 1.0);
        KeyFrame keyFrame = new KeyFrame(duration, keyValueX, keyValueY);
        timeline.getKeyFrames().add(keyFrame);

        return timeline;
    }

    /**
     * Create a scale out animation
     */
    public static Timeline scaleOut(Node node, Duration duration) {
        Timeline timeline = new Timeline();
        KeyValue keyValueX = new KeyValue(node.scaleXProperty(), 0.0);
        KeyValue keyValueY = new KeyValue(node.scaleYProperty(), 0.0);
        KeyFrame keyFrame = new KeyFrame(duration, keyValueX, keyValueY);
        timeline.getKeyFrames().add(keyFrame);

        return timeline;
    }
}
