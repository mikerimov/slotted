/*
 * Copyright 2012 Jeffrey Kleiss
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.googlecode.slotted.client;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.user.client.History;

import java.util.HashMap;

/**
 * HistoryMapper is an abstract base class that manages generation and parsing of History Tokens.
 * This class is extended by every implementation of Slotted and defines which SlottedPlaces need
 * URL management.
 */
abstract public class HistoryMapper {

    private HashMap<String, SlottedPlace> nameToPlaceMap = new HashMap<String, SlottedPlace>();
    private HashMap<Class, String> placeToNameMap = new HashMap<Class, String>();
    private SlottedPlace defaultPlace;
    private SlottedController controller;
    private PlaceHistoryMapper legacyHistoryMapper;
    private boolean handlingHistory;

    /**
     * Default constructor which adds itself as a History listener and calls init() on the base
     * class to register all SlottedPlaces.
     */
    public HistoryMapper() {
        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override public void onValueChange(ValueChangeEvent<String> event) {
                handleHistory(event.getValue());
            }
        });

        init();
        if (defaultPlace == null) {
            System.out.println("WARNING: Default place not set.");
        }
    }

    /**
     * Called by the Constructor to register all the SlottedPlaces that will be managed.
     *
     * @see #registerDefaultPlace(SlottedPlace)
     * @see #registerPlace(SlottedPlace)
     */
    protected abstract void init();

    /**
     * Called by SlottedController to provide the circular reference.
     */
    protected void setController(SlottedController controller) {
        this.controller = controller;
    }

    /**
     * Called by SlottedController to provide legacy HistoryMapper to process Places not migrated
     * to Slotted framework.
     */
    protected void setLegacyHistoryMapper(PlaceHistoryMapper legacyHistoryMapper) {
        this.legacyHistoryMapper = legacyHistoryMapper;
    }

    /**
     * Returns the SlottedPlace instance that represents the place to navigate to if the history
     * token is empty.
     */
    public SlottedPlace getDefaultPlace() {
        return defaultPlace;
    }

    /**
     * Registers the passed SlottedPlace as the default location to navigate to if the history
     * token is empty.  It also registers the SlottedPlace as a normal page.
     *
     * @param place The place with correct parameters to display.
     * @throws IllegalStateException if default place has already be registered.
     * @see #registerPlace(SlottedPlace)
     */
    public void registerDefaultPlace(SlottedPlace place) {
        if (defaultPlace != null) {
            throw new IllegalStateException("Default place already set.");
        }
        defaultPlace = place;
        registerPlace(place);
    }

    /**
     * Same as {@link #registerDefaultPlace(SlottedPlace)}, but allows Places URL token to be
     * overridden, instead of using the simple class name.
     *
     * @param place The place with correct parameters to display.
     * @param name The new URL token to display in the History token, must be URL safe.
     */
    public void registerDefaultPlace(SlottedPlace place, String name) {
        if (defaultPlace != null) {
            throw new IllegalStateException("Default place already set.");
        }
        defaultPlace = place;
        registerPlace(place, name);
    }

    /**
     * Registers the passed SlottedPlace be handled in generating and parsing History tokens.  The
     * SlottedPlace's simple class name will be used in the HistoryToken.  If the name ends with
     * "Place", that will be stripped off.  (i.e. "HomePlace" will appear as "Home" in the token)
     *
     * @param place The place instance to be managed.  The parameters aren't important, because
     *              they are replaced during parsing.
     * @see #registerPlace(SlottedPlace, String)
     */
    public void registerPlace(SlottedPlace place) {
        String name = place.getClass().getName();
        int index = name.lastIndexOf(".");
        if (name.endsWith("Place")) {
            name = name.substring(index + 1, name.length() - 5);
        } else {
            name = name.substring(index + 1);
        }

        registerPlace(place, name);
    }

    /**
     * Same as {@link #registerPlace(SlottedPlace)}, but allows Places URL token to be
     * overridden, instead of using the simple class name.
     *
     * @param place The place with correct parameters to display.
     * @param name The new URL token to display in the History token, must be URL safe.
     */
    public void registerPlace(SlottedPlace place, String name) {
        nameToPlaceMap.put(name, place);
        placeToNameMap.put(place.getClass(), name);
    }

    /**
     * Called by SlottedController to provide a URL empty history token when navigating to the
     * default place.
     */
    protected void goToDefaultPlace() {
        History.newItem("", true);
    }

    /**
     * Called by the History listener to parse and navigate the new history token.
     *
     * @param token History Token that needs to be navigated to.
     */
    protected void handleHistory(String token) {
        RuntimeException parsingException = null;
        handlingHistory = true;
        if (token == null || token.trim().isEmpty()) {
            if (defaultPlace == null) {
                throw new IllegalStateException("No default place defined.  Make sure that " +
                        "registerDefaultPlace() is called");
            }
            controller.goTo(defaultPlace);
        } else {
            try {
                PlaceParameters parameters = new PlaceParameters();

                String[] split = token.split("\\?");
                String[] placeNames = split[0].split("/");

                if (split.length > 1) {
                    String[] paramPairs = split[1].split("&");

                    for (String pair: paramPairs) {
                        String[] pairSplit = pair.split("=");
                        parameters.setParameter(pairSplit[0], pairSplit[1]);
                    }
                }

                SlottedPlace[] places = new SlottedPlace[placeNames.length];
                for (int i = 0; i < places.length; i++) {
                    places[i] = nameToPlaceMap.get(placeNames[i]);
                    if (places[i] == null) {
                        throw new IllegalStateException("Place not defined:" + placeNames[i]);
                    }
                    places[i].setPlaceParameters(parameters);
                }

                controller.goTo(places[0], places);
            } catch (RuntimeException e) {
                parsingException = e;
            }
        }

        if (parsingException != null) {
            if (legacyHistoryMapper != null) {
                Place place = legacyHistoryMapper.getPlace(token);
                controller.goTo(place);
            } else {
                throw parsingException;
            }
        }

        handlingHistory = false;
    }

    /**
     * Creates a History token for the passed place, which can be used in links or other navigation
     * URLs.
     *
     * @param place The SlottedPlace that will be navigated to.
     * @param parameters The parameters that should be added to the token.
     * @return History token that can be added to a base URL for navigation.
     */
    public String createToken(SlottedPlace place, PlaceParameters parameters) {
        String token = placeToNameMap.get(place.getClass());

        if (parameters != null) {
            token += parameters.toString();
        }

        return token;
    }

    /**
     * Generates the History token for the currently display Places.
     */
    public void createToken() {
        if (!handlingHistory) {
            String token = createToken(controller.getRoot());

            History.newItem(token, false);
        }
    }

    protected String createToken(ActiveSlot activeSlot) {
        String token;
        if (activeSlot.getPlace() instanceof WrappedPlace) {
            Place place = ((WrappedPlace) activeSlot.getPlace()).getPlace();
            token = legacyHistoryMapper.getToken(place);

        } else {
            token = createPageList(activeSlot);

            PlaceParameters parameters = controller.getCurrentParameters();
            if (parameters != null) {
                token += parameters.toString();
            }
        }

        return token;
    }

    private String createPageList(ActiveSlot activeSlot) {
        String token = placeToNameMap.get(activeSlot.getPlace().getClass());
        if (token == null) {
            throw new IllegalStateException("Place not registered:" + activeSlot.getPlace().getClass().getName());
        }

        for (ActiveSlot child: activeSlot.getChildren()) {
            token += "/" + createPageList(child);
        }
        return token;
    }

}