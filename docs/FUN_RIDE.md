# Fun ride in Standard RideDeck

Plan while parked from Setup > Navigation > Go for a fun ride. Choose a starting place or fresh current location, riding duration, return-to-start or another finish, exploration direction, and up to two real stops. A named place or decimal coordinates can be entered. Chosen stops replace exploration guesses.

The planner uses the same v1 itinerary meanings and seed calculation as Yamaha. Standard uses the public Valhalla motorcycle router and does not claim winding-road scoring, traffic, weather, verified scenery, parking or legal access. At most three sequential requests are sent, at least 1.2 seconds apart. Cancel discards late results and prevents further requests. Actual durations between 40 and 200 percent of the target are displayed prominently; unsuitable and duplicate road choices are omitted. A target is not a guaranteed duration.

The large native map shows actual routing geometry, start S, intermediate stops 1 and 2, and finish F (S/F for a loop). Drag, zoom, fit, or expand the map. OpenStreetMap tiles are fetched only for the displayed viewport, with one network worker and at most twelve pending tiles; queued tiles outside the current viewport are skipped. The identifiable User-Agent includes the project address. Disk caching honors Cache-Control max-age, Expires and no-store, with a seven-day fallback. There is no offline prefetch. Chosen locations go to the public routing service; map tile coordinates go to OpenStreetMap. Shared public services have fair-use and availability limits.

Only the installed Google Maps Android package is offered for external handoff. The URL contains origin, every intermediate coordinate in order, and final destination. There is no implicit browser or other-app fallback. No external app is advertised as preserving the preview geometry: Google Maps recalculates between stops. The user must review all stops and roads in its preview before starting. It can change duration, road choices and available travel mode. Motorway/toll/ferry avoidance is passed where supported; unpaved-road exclusion and fewer-U-turn ranking do not transfer. Two-wheeler coverage varies.

On 19 October 2026, the owner manually opened the handoff on a Xiaomi 14T Pro running Google Maps 26.41.03.993332563 (version code 1068821134). Inspection of the expanded stop list confirmed both intermediate public Bangkok test coordinates in order, the original point as final destination, and selected two-wheeler mode. Maps recalculated the test route to 34 km and 67 minutes. This verifies this app version and tested region; it does not guarantee future Maps versions, all regional travel modes, exact preview geometry or road preferences.

The draft persists locally. Rotation preserves selected route and full geometry through a private file rather than a large activity-state bundle. Cancel/back does not change a running external navigation app. This feature does not add built-in navigation to Standard.

References:

- https://developers.google.com/maps/documentation/urls/get-started
- https://valhalla.github.io/valhalla/api/route/api-reference/
- https://operations.osmfoundation.org/policies/tiles/
