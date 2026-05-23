package com.ossobo.winterfx.notifications.position;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Window;

/**
 * PositionManager v1.0
 * Calcula coordenadas para posicionamento da notificação.
 */
public final class PositionManager {

    private Window defaultOwner;
    private double offsetX = 20;
    private double offsetY = 20;

    public PositionManager() {}

    public PositionManager(Window defaultOwner) {
        this.defaultOwner = defaultOwner;
    }

    public Point2D calculate(ScreenPosition position, Window owner, double popupWidth, double popupHeight) {
        Window targetOwner = owner != null ? owner : defaultOwner;

        return switch (position) {
            case CENTER -> calculateCenter(targetOwner, popupWidth, popupHeight);
            case TOP_LEFT -> calculateTopLeft(targetOwner, popupWidth, popupHeight);
            case TOP_CENTER -> calculateTopCenter(targetOwner, popupWidth, popupHeight);
            case TOP_RIGHT -> calculateTopRight(targetOwner, popupWidth, popupHeight);
            case BOTTOM_LEFT -> calculateBottomLeft(targetOwner, popupWidth, popupHeight);
            case BOTTOM_CENTER -> calculateBottomCenter(targetOwner, popupWidth, popupHeight);
            case BOTTOM_RIGHT -> calculateBottomRight(targetOwner, popupWidth, popupHeight);
            case OWNER_CENTER -> calculateOwnerCenter(targetOwner, popupWidth, popupHeight);
            case OWNER_TOP -> calculateOwnerTop(targetOwner, popupWidth, popupHeight);
            case OWNER_BOTTOM -> calculateOwnerBottom(targetOwner, popupWidth, popupHeight);
            case MOUSE_POSITION -> calculateMousePosition(popupWidth, popupHeight);
            default -> new Point2D(100, 100);
        };
    }

    private Point2D calculateCenter(Window owner, double width, double height) {
        Rectangle2D bounds = getScreenBounds(owner);
        double x = bounds.getMinX() + (bounds.getWidth() - width) / 2;
        double y = bounds.getMinY() + (bounds.getHeight() - height) / 2;
        return new Point2D(x, y);
    }

    private Point2D calculateTopRight(Window owner, double width, double height) {
        Rectangle2D bounds = getScreenBounds(owner);
        double x = bounds.getMaxX() - width - offsetX;
        double y = bounds.getMinY() + offsetY;
        return new Point2D(x, y);
    }

    private Point2D calculateTopLeft(Window owner, double width, double height) {
        Rectangle2D bounds = getScreenBounds(owner);
        double x = bounds.getMinX() + offsetX;
        double y = bounds.getMinY() + offsetY;
        return new Point2D(x, y);
    }

    private Point2D calculateBottomRight(Window owner, double width, double height) {
        Rectangle2D bounds = getScreenBounds(owner);
        double x = bounds.getMaxX() - width - offsetX;
        double y = bounds.getMaxY() - height - offsetY;
        return new Point2D(x, y);
    }

    private Point2D calculateBottomLeft(Window owner, double width, double height) {
        Rectangle2D bounds = getScreenBounds(owner);
        double x = bounds.getMinX() + offsetX;
        double y = bounds.getMaxY() - height - offsetY;
        return new Point2D(x, y);
    }

    private Point2D calculateTopCenter(Window owner, double width, double height) {
        Rectangle2D bounds = getScreenBounds(owner);
        double x = bounds.getMinX() + (bounds.getWidth() - width) / 2;
        double y = bounds.getMinY() + offsetY;
        return new Point2D(x, y);
    }

    private Point2D calculateBottomCenter(Window owner, double width, double height) {
        Rectangle2D bounds = getScreenBounds(owner);
        double x = bounds.getMinX() + (bounds.getWidth() - width) / 2;
        double y = bounds.getMaxY() - height - offsetY;
        return new Point2D(x, y);
    }

    private Point2D calculateOwnerCenter(Window owner, double width, double height) {
        if (owner == null) return calculateCenter(null, width, height);
        double x = owner.getX() + (owner.getWidth() - width) / 2;
        double y = owner.getY() + (owner.getHeight() - height) / 2;
        return new Point2D(x, y);
    }

    private Point2D calculateOwnerTop(Window owner, double width, double height) {
        if (owner == null) return calculateTopCenter(null, width, height);
        double x = owner.getX() + (owner.getWidth() - width) / 2;
        double y = owner.getY() - height - 10;
        return new Point2D(x, y);
    }

    private Point2D calculateOwnerBottom(Window owner, double width, double height) {
        if (owner == null) return calculateBottomCenter(null, width, height);
        double x = owner.getX() + (owner.getWidth() - width) / 2;
        double y = owner.getY() + owner.getHeight() + 10;
        return new Point2D(x, y);
    }

    private Point2D calculateMousePosition(double width, double height) {
        // Obtém posição do mouse via evento mais recente
        return new Point2D(100, 100); // Placeholder
    }

    private Rectangle2D getScreenBounds(Window owner) {
        if (owner != null) {
            return Screen.getScreensForRectangle(
                            owner.getX(), owner.getY(),
                            owner.getWidth(), owner.getHeight()
                    ).stream().findFirst()
                    .map(Screen::getVisualBounds)
                    .orElse(Screen.getPrimary().getVisualBounds());
        }
        return Screen.getPrimary().getVisualBounds();
    }

    public void setOffset(double x, double y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    public void setDefaultOwner(Window owner) {
        this.defaultOwner = owner;
    }
}
