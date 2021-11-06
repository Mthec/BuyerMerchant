package com.wurmonline.server.questions;

import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import mod.wurmunlimited.WurmTradingQuestionTest;
import mod.wurmunlimited.buyermerchant.db.BuyerScheduler;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static mod.wurmunlimited.Assert.containsNoneOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateScheduleQuestionTests extends WurmTradingQuestionTest {

    private void askQuestion() {
        super.askQuestion(new UpdateScheduleQuestion(owner, buyer));
    }

    @Test
    void testRemoveItemFromList() throws NoSuchTemplateException, BuyerScheduler.UpdateAlreadyExists, SQLException {
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(ItemList.hatchet);
        for (int i = 1; i < 15; ++i) {
            BuyerScheduler.addUpdateFor(buyer, template, (byte)i, -1, 1.0f, i, 1, 1, true, 1);
        }

        askQuestion();

        Matcher matcher = Pattern.compile("id=\"(\\d+)i\";").matcher(com.lastBmlContent);
        Set<Integer> deleted = new HashSet<>();
        while (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            if (id > 4 && id < 8) {
                deleted.add(id);
                answers.setProperty(id + "remove", "true");
            }
        }
        answers.setProperty("save", "true");
        answer();

        BuyerScheduler.Update[] updates = BuyerScheduler.getUpdatesFor(buyer);
        assertEquals(11, updates.length);
        assertThat(Arrays.stream(updates).mapToInt(BuyerScheduler.Update::getPrice).boxed().collect(Collectors.toSet()), containsNoneOf(deleted));
    }
}
