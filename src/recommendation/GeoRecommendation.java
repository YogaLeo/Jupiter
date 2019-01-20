package recommendation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Event;

public class GeoRecommendation {

	public List<Event> recommendItems(String userId, double lat, double lon) {
		List<Event> recommendedItems = new ArrayList<>();

		// Step 1, get all favorited itemids
		DBConnection connection = DBConnectionFactory.getConnection();
		Set<String> favoritedItemIds = connection.getFavoriteItemIds(userId);

		// Step 2, get all categories,
		// {"music": 3, "sports": 5, "art": 2}
		Map<String, Integer> allCategories = new HashMap<>();
		for (String itemId : favoritedItemIds) {
			Set<String> categories = connection.getCategories(itemId);
			for (String category : categories) {
				allCategories.put(category, allCategories.getOrDefault(category, 0) + 1);
			}
		}

		// sort by count
		// {"sports": 5, "music": 3, "art": 2}
		List<Entry<String, Integer>> categoryList = new ArrayList<>(allCategories.entrySet());
		Collections.sort(categoryList, (Entry<String, Integer> e1, Entry<String, Integer> e2) -> {
			return Integer.compare(e2.getValue(), e1.getValue());
		});

		// Step 3, search based on category, filter out favorite items
		Set<String> visitedItemIds = new HashSet<>(); // 查找时多次query api 产生的重复
		for (Entry<String, Integer> category : categoryList) {
			List<Event> items = connection.searchItems(lat, lon, category.getKey());
			for (Event item : items) {
				//deduplicate favoritedItemIds: 以前已经收藏的重复
				if (!favoritedItemIds.contains(item.getItemId()) && !visitedItemIds.contains(item.getItemId())) {
					recommendedItems.add(item);
					visitedItemIds.add(item.getItemId());
				}
			}
		}

		connection.close();
		return recommendedItems;
	}

}
