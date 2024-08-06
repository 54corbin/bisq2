/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.components.table;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class BisqTableView<T> extends TableView<T> {
    private final static double TABLE_HEADER_HEIGHT = 36;
    private final static double TABLE_ROW_HEIGHT = 54;
    private final static double TABLE_H_SCROLLBAR_HEIGHT = 16;
    private final static double TABLE_V_SCROLLBAR_WIDTH = 16;

    @Getter
    private final SortedList<T> sortedList;
    private ListChangeListener<T> listChangeListener;
    private ChangeListener<Number> widthChangeListener;
    private final boolean useComparatorBinding;
    // If set we use the sum of the minWidth values of all visible columns to set the minWidth of the tableView.
    @Setter
    private boolean deriveMinWidthFromColumns = true;

    @Getter
    private final ObjectProperty<ScrollBar> horizontalScrollbar = new SimpleObjectProperty<>();
    @Getter
    private final ObjectProperty<ScrollBar> verticalScrollbar = new SimpleObjectProperty<>();
    private Subscription verticalScrollbarVisiblePin;
    private Subscription verticalScrollbarPin;

    public BisqTableView(ObservableList<T> list) {
        this(new SortedList<>(list));
    }

    public BisqTableView(ObservableList<T> list, boolean useComparatorBinding) {
        this(new SortedList<>(list), useComparatorBinding);
    }

    public BisqTableView(SortedList<T> sortedList) {
        this(sortedList, true);
    }

    public BisqTableView(SortedList<T> sortedList, boolean useComparatorBinding) {
        super(sortedList);
        this.sortedList = sortedList;
        this.useComparatorBinding = useComparatorBinding;

        if (useComparatorBinding) {
            // Need to bind early as otherwise table not applying it
            sortedList.comparatorProperty().bind(this.comparatorProperty());
        }
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    public void initialize() {
        if (useComparatorBinding) {
            sortedList.comparatorProperty().unbind();
            sortedList.comparatorProperty().bind(this.comparatorProperty());

            if (deriveMinWidthFromColumns) {
                verticalScrollbarPin = EasyBind.subscribe(verticalScrollbar, scrollBar -> {
                    if (scrollBar != null) {

                        verticalScrollbarVisiblePin = EasyBind.subscribe(scrollBar.visibleProperty(), scrollBarVisible -> {
                            if (scrollBarVisible != null) {
                                adjustMinWidth();
                            }
                        });
                    }
                });

                // We get verticalScrollbar property set if we find one after some delayed iterations
                recursiveFindScrollbar(this, Orientation.VERTICAL);
            }
        }
    }

    public void removeListeners() {
        if (listChangeListener != null) {
            getItems().removeListener(listChangeListener);
            listChangeListener = null;
        }
        if (widthChangeListener != null) {
            widthProperty().removeListener(widthChangeListener);
            widthChangeListener = null;
        }
        if (verticalScrollbarPin != null) {
            verticalScrollbarPin.unsubscribe();
        }
        if (verticalScrollbarVisiblePin != null) {
            verticalScrollbarVisiblePin.unsubscribe();
        }
    }

    public void dispose() {
        removeListeners();

        if (useComparatorBinding) {
            sortedList.comparatorProperty().unbind();
        }
        horizontalScrollbar.set(null);
        verticalScrollbar.set(null);
    }

    public void setPlaceholderText(String placeHolderText) {
        setPlaceholder(new Label(placeHolderText));
    }

    public void setFixHeight(double value) {
        setMinHeight(value);
        setMaxHeight(value);
    }

    public void adjustHeightToNumRows() {
        adjustHeightToNumRows(Integer.MAX_VALUE);
    }

    public void adjustHeightToNumRows(int maxNumItems) {
        adjustHeightToNumRows(TABLE_H_SCROLLBAR_HEIGHT, TABLE_HEADER_HEIGHT, TABLE_ROW_HEIGHT, maxNumItems);
    }

    public void adjustHeightToNumRows(double scrollbarHeight, double headerHeight, double rowHeight, int maxNumItems) {
        if (listChangeListener != null) {
            getItems().removeListener(listChangeListener);
            listChangeListener = null;
        }
        if (widthChangeListener != null) {
            widthProperty().removeListener(widthChangeListener);
            widthChangeListener = null;
        }
        listChangeListener = c -> {
            adjustHeight(scrollbarHeight, headerHeight, rowHeight, maxNumItems);
            UIThread.runOnNextRenderFrame(() -> adjustHeight(scrollbarHeight, headerHeight, rowHeight, maxNumItems));
        };
        getItems().addListener(listChangeListener);

        widthChangeListener = (observable, oldValue, newValue) -> {
            adjustHeight(scrollbarHeight, headerHeight, rowHeight, maxNumItems);
            UIThread.runOnNextRenderFrame(() -> adjustHeight(scrollbarHeight, headerHeight, rowHeight, maxNumItems));
        };
        widthProperty().addListener(widthChangeListener);

        adjustHeight(scrollbarHeight, headerHeight, rowHeight, maxNumItems);
    }

    public void hideVerticalScrollbar() {
        // As we adjust height we do not need the vertical scrollbar.
        getStyleClass().add("hide-vertical-scrollbar");
    }

    public void allowVerticalScrollbar() {
        getStyleClass().remove("hide-vertical-scrollbar");
    }

    public void hideHorizontalScrollbar() {
        getStyleClass().add("force-hide-horizontal-scrollbar");
    }

    public void allowHorizontalScrollbar() {
        getStyleClass().remove("force-hide-horizontal-scrollbar");
    }


    private void adjustMinWidth() {
        setMinWidth(sumOfColumns() + scrollbarWidth());
    }

    private Double scrollbarWidth() {
        return Optional.ofNullable(verticalScrollbar.get())
                .filter(Node::isVisible)
                .map(s -> TABLE_V_SCROLLBAR_WIDTH)
                .orElse(0d);
    }

    private double sumOfColumns() {
        return getColumns().stream()
                .filter(TableColumnBase::isVisible)
                .mapToDouble(TableColumnBase::getMinWidth)
                .sum();
    }

    private void adjustHeight(double scrollbarHeight, double headerHeight, double rowHeight, int maxNumItems) {
        int size = getItems().size();
        int numItems = Math.min(maxNumItems, size);
        if (size > numItems) {
            allowVerticalScrollbar();
        } else {
            hideVerticalScrollbar();
        }
        UIThread.runOnNextRenderFrame(this::adjustMinWidth);
        if (numItems == 0) {
            return;
        }
        double realScrollbarHeight = findScrollbar(BisqTableView.this, Orientation.HORIZONTAL)
                .map(e -> scrollbarHeight)
                .orElse(0d);
        double height = headerHeight + numItems * rowHeight + realScrollbarHeight;
        setFixHeight(height);
    }

    public BisqTableColumn<T> getSelectionMarkerColumn() {
        return new BisqTableColumn.Builder<T>()
                .fixWidth(3)
                .setCellFactory(getSelectionMarkerCellFactory())
                .isSortable(false)
                .build();
    }

    public Callback<TableColumn<T, T>, TableCell<T, T>> getSelectionMarkerCellFactory() {
        return column -> new TableCell<>() {
            private Subscription selectedPin;

            @Override
            public void updateItem(final T item, boolean empty) {
                super.updateItem(item, empty);

                // Clean up previous row
                if (getTableRow() != null && selectedPin != null) {
                    selectedPin.unsubscribe();
                }

                // Set up new row
                TableRow<T> newRow = getTableRow();
                if (newRow != null) {
                    selectedPin = EasyBind.subscribe(newRow.selectedProperty(), isSelected ->
                            setId(isSelected ? "selection-marker" : null)
                    );
                }
            }
        };
    }

    public static void recursiveFindScrollbar(BisqTableView<?> tableView, Orientation orientation) {
        ObjectProperty<ScrollBar> scrollbar = orientation == Orientation.HORIZONTAL
                ? tableView.getHorizontalScrollbar()
                : tableView.getVerticalScrollbar();
        recursiveFindScrollbar(tableView, orientation, scrollbar, new AtomicInteger(0), 10);
    }

    public static void recursiveFindScrollbar(BisqTableView<?> tableView,
                                              Orientation orientation,
                                              ObjectProperty<ScrollBar> scrollBar,
                                              AtomicInteger numIterations,
                                              int maxIterations) {
        Optional<ScrollBar> candidate = findScrollbar(tableView, orientation);
        if (candidate.isPresent()) {
            scrollBar.set(candidate.get());
        } else if (numIterations.incrementAndGet() < maxIterations) {
            UIScheduler.run(() ->
                            recursiveFindScrollbar(tableView, orientation, scrollBar, numIterations, maxIterations))
                    .after(100);
        }
    }

    public static Optional<ScrollBar> findScrollbar(BisqTableView<?> tableView, Orientation orientation) {
        ObjectProperty<ScrollBar> cachedScrollbar = orientation == Orientation.HORIZONTAL
                ? tableView.getHorizontalScrollbar()
                : tableView.getVerticalScrollbar();
        if (cachedScrollbar.get() != null) {
            return Optional.of(cachedScrollbar.get());
        }

        Optional<ScrollBar> scrollbar = tableView.lookupAll(".scroll-bar").stream()
                .filter(node -> node instanceof ScrollBar)
                .map(node -> (ScrollBar) node)
                .filter(scrollBar -> scrollBar.getOrientation().equals(orientation))
                .filter(Node::isVisible)
                .findAny();
        scrollbar.ifPresent(cachedScrollbar::set);

        return scrollbar;
    }
}