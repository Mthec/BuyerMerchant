package com.wurmonline.server.questions;

import com.wurmonline.server.behaviours.MethodsItems;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.items.*;
import com.wurmonline.shared.constants.ItemMaterials;
import mod.wurmunlimited.WurmTradingQuestionTest;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wurmonline.server.items.ItemList.backPack;
import static mod.wurmunlimited.Assert.bmlNotEqual;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AddItemToBuyerQuestionTest extends WurmTradingQuestionTest {

    private static String cancel = "You decide not to add anything.";
    private Pattern allOptions = Pattern.compile("options=\"([\\s-,\\w']+)\"");
    private Pattern otherOptions = Pattern.compile("options=\"(?:Nothing,)?(?:No change,)?(?:Any material,)?([\\s-,\\w']+)\"");

    private void askQuestion() {
        super.askQuestion(new AddItemToBuyerQuestion(owner, buyer.getWurmId()));
    }

    private String getElementPositionInOptions(String bml, int templateId) {
        try {
            String templateName = ItemTemplateFactory.getInstance().getTemplate(templateId).getName();
            Matcher match = otherOptions.matcher(bml);
            assertTrue(match.find());
            String[] elements = match.group(1).split(",");
            for (int i = 0; i < elements.length; ++i) {
                if (elements[i].startsWith(templateName))
                    return Integer.toString(i + 1);
            }
            throw new RuntimeException("No such option found - " + templateName);
        } catch (NoSuchTemplateException e) {
            throw new RuntimeException(e);
        }
    }

    private String getElementPositionInOptions(String bml, String element) {
        Matcher match = otherOptions.matcher(bml);
        assertTrue(match.find());
        List<String> elements = Arrays.asList(match.group(1).split(","));

        if (element.equals("unknown")) {
            if (elements.size() > 1)
                return Integer.toString(0);
            else
                throw new RuntimeException("unknown (Any material) is a not a valid option when the list only has one element.");
        }

        return Integer.toString(elements.indexOf(element) + 1);
    }

    private void filterAndAnswer(String filter) {
        answers.setProperty("filterme", "true");
        answers.setProperty("filtertext", filter);
        answer();
    }

    @Test
    void testBmlSent() {
        askQuestion();
        assertNotEquals(FakeCommunicator.empty, factory.getCommunicator(sender).lastBmlContent);
    }

    @Test
    void testBmlSame() {
        // Except for passthrough id.
        askQuestion();
        askQuestion();
        String[] bml = factory.getCommunicator(sender).getBml();

        assertEquals(removePassThrough(bml[0]), removePassThrough(bml[1]));
    }

    @Test
    void testCancel() {
        askQuestion();

        answers.setProperty("cancel", "true");
        answer();

        assertThat(sender, receivedMessageContaining(cancel));
    }

    @Test
    void testFilterUsed() {
        askQuestion();
        filterAndAnswer("blahblahblah");

        assertThat(sender, bmlNotEqual());
    }

    @Test
    void testItemTemplateFilter() {
        String templateName = "hatchet - iron ";
        askQuestion();
        filterAndAnswer("hatchet");

        Matcher match = otherOptions.matcher(factory.getCommunicator(sender).lastBmlContent);
        assertTrue(match.find());
        String name = match.group(1);
        assertEquals(templateName, name);
    }

    @Test
    void testMaterialFilter() {
        String materialName = "brass";
        askQuestion();
        assert com.lastBmlContent.contains("hatchet");
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, 7));
        answer();

        filterAndAnswer(materialName);

        Matcher match = otherOptions.matcher(factory.getCommunicator(sender).lastBmlContent);
        assertTrue(match.find());
        String name = match.group(1);
        assertEquals(materialName, name);
    }

    @Test
    void testOnFilterSingleItemDoesNotQuit() {
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate("backpack");
        String name = template.getName();
        askQuestion();
        filterAndAnswer(name);
        Matcher matcher = otherOptions.matcher(com.lastBmlContent);
        if (!matcher.find()) throw new AssertionError();
        assert matcher.group(1).equals(template.getName());
        answers.setProperty("templateId", "0");
        answer();

        matcher = otherOptions.matcher(com.lastBmlContent);
        assertTrue(matcher.find());
        assertEquals(Item.getMaterialString(template.getMaterial()), matcher.group(1));

        assertNotEquals(cancel, com.lastNormalServerMessage);
    }

    @Test
    void testOnFilterSingleMaterialDoesNotQuit() {
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate("backpack");
        String name = template.getName();
        String material = Item.getMaterialString(template.getMaterial());
        askQuestion();
        filterAndAnswer(name);
        answers.setProperty("templateId", "0");
        answer();
        filterAndAnswer(material);
        answers.setProperty("material", "0");
        answer();

        Matcher matcher = itemAndMaterial.matcher(com.lastBmlContent);
        assertTrue(matcher.find());
        assertEquals(name, matcher.group(1));
        assertEquals(material, matcher.group(2));

        assertNotEquals(cancel, com.lastNormalServerMessage);
    }

    @Test
    void testFilterItemNoResultsQuits() {
        askQuestion();
        filterAndAnswer("blahblahblah");

        Matcher matcher = itemAndMaterial.matcher(com.lastBmlContent);
        assertFalse(matcher.find());

        answers.setProperty("templateId", "0");
        answer();

        assertEquals(cancel, com.lastNormalServerMessage);
    }

    @Test
    void testFilterMaterialNoResultsIsDefault() {
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate("hatchet");
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, template.getTemplateId()));
        answer();

        filterAndAnswer("blahblahblah");

        Matcher matcher = itemAndMaterial.matcher(com.lastBmlContent);
        assertFalse(matcher.find());

        answers.setProperty("material", "0");
        answer();

        matcher = itemAndMaterial.matcher(com.lastBmlContent);
        assertTrue(matcher.find());
        assertEquals(template.getName(), matcher.group(1));
        assertEquals(Item.getMaterialString(template.getMaterial()), matcher.group(2));

        assertNotEquals(cancel, com.lastNormalServerMessage);
    }

    @Test
    void testFilterUnknownMaterialNoResultsIsAny() throws NoSuchTemplateException {
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(factory.getIsUnknownMaterial());
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, template.getTemplateId()));
        answer();

        filterAndAnswer("blahblahblah");

        Matcher matcher = itemAndMaterial.matcher(com.lastBmlContent);
        assertFalse(matcher.find());

        answers.setProperty("material", "0");
        answer();

        matcher = itemAndMaterial.matcher(com.lastBmlContent);
        assertTrue(matcher.find());
        assertEquals(template.getName(), matcher.group(1));
        assertEquals("Any", matcher.group(2));

        assertNotEquals(cancel, com.lastNormalServerMessage);
    }

    // TODO - More Filter tests.

    @Test
    void testBack() {
        // New Question - Back - Default Template Selected
        askQuestion();
        factory.attachFakeCommunicator(sender);
        answers.setProperty("templateId", "7");
        answer();
        
        answers.setProperty("back", "true");
        answer();

        assertThat(sender, bmlNotEqual());
    }

    @Test
    void testFullMaterialsList() {
        askQuestion();
        answers.setProperty("templateId", "7");
        answer();
        
        answers.setProperty("list_all_materials", "true");
        answer();

        String[] bml = factory.getCommunicator(sender).getBml();

        assertTrue(bml[1].length() < bml[2].length());
    }

    @Test
    void testDefaultTemplateSelectedOnBack() {
        askQuestion();
        String template7Index = getElementPositionInOptions(com.lastBmlContent, 7);
        answers.setProperty("templateId", template7Index);
        answer();
        
        answers.setProperty("back", "true");
        answer();

        Matcher match = defaultOption.matcher(factory.getCommunicator(sender).lastBmlContent);
        assertTrue(match.find());
        String id = match.group(1);
        assertEquals(template7Index, id);
    }

    @Test
    void testItemWithCorrectDetailsAddedToPriceList() throws PriceList.NoPriceListOnBuyer, NoSuchTemplateException {
        int templateId = 7;
        byte material = ItemTemplateFactory.getInstance().getTemplate(templateId).getMaterial();
        int weight = 1000;
        float ql = 55.6f;
        int money = 1122334455;
        int minimumPurchase = 100;
        Change change = new Change(money);

        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, templateId));
        answer();

        answers.setProperty("material", getElementPositionInOptions(com.lastBmlContent, Item.getMaterialString(material)));
        answer();

        answers.setProperty("weight", WeightString.toString(weight));
        answers.setProperty("q", Float.toString(ql));
        answers.setProperty("g", Long.toString(change.goldCoins));
        answers.setProperty("s", Long.toString(change.silverCoins));
        answers.setProperty("c", Long.toString(change.copperCoins));
        answers.setProperty("i", Long.toString(change.ironCoins));
        answers.setProperty("p", Integer.toString(minimumPurchase));
        answer();

        PriceList.Entry item = PriceList.getPriceListFromBuyer(buyer).iterator().next();
        Change price = new Change(item.getPrice());

        assertAll(
                () -> assertEquals(templateId, item.getItem().getTemplateId(), "Template Id incorrect"),
                () -> assertEquals(material, item.getItem().getMaterial(), "Material incorrect"),
                () -> assertEquals(weight, item.getItem().getWeightGrams(), "Weight incorrect"),
                () -> assertEquals(ql, item.getItem().getQualityLevel(), "QL incorrect"),
                () -> assertEquals(change.goldCoins, price.goldCoins, "Gold incorrect"),
                () -> assertEquals(change.silverCoins, price.silverCoins, "Silver incorrect"),
                () -> assertEquals(change.copperCoins, price.copperCoins, "Copper incorrect"),
                () -> assertEquals(change.ironCoins, price.ironCoins, "Iron incorrect"),
                () -> assertEquals(minimumPurchase, item.getMinimumPurchase(), "Minimum Purchase incorrect")
        );
    }


    private void testMaterialSelectionOptions(int templateId, int count) {
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, templateId));
        answer();
        Matcher match = otherOptions.matcher(com.lastBmlContent);
        assertTrue(match.find());
        String options = match.group(1);

        assertEquals(count, options.split(",").length);
    }

    @Test
    void testMaterialSelectionOptions_Meat() {
        testMaterialSelectionOptions(factory.getIsMeatId(), AddItemToBuyerQuestion.allMeat.length);
    }

    @Test
    void testMaterialSelectionOptions_Metal() {
        testMaterialSelectionOptions(factory.getIsMetalId(), MethodsItems.getAllMetalTypes().length);
    }

    @Test
    void testMaterialSelectionOptions_Wood() {
        testMaterialSelectionOptions(factory.getIsWoodId(), MethodsItems.getAllNormalWoodTypes().length);
    }

    @Test
    void testMaterialSelectionOptions_DefaultAndCustom() {
        testMaterialSelectionOptions(factory.getIsDefaultMaterialId(), 1);

        int count = 0;
        for (byte x = 1; x <= ItemMaterials.MATERIAL_MAX; ++x) {
            if (!Item.getMaterialString(x).equals("unknown"))
                ++count;
        }

        
        answers.setProperty("list_all_materials", "true");
        answer();
        Matcher match = otherOptions.matcher(factory.getCommunicator(sender).lastBmlContent);
        assertTrue(match.find());
        String options = match.group(1);

        assertEquals(count, options.split(",").length);
    }

    private void testMaterialTypeCorrect(int templateId, byte material, boolean custom) throws PriceList.NoPriceListOnBuyer {
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, templateId));
        answer();
        if (custom) {
            answers.setProperty("list_all_materials", "true");
            answer();
        }
        
        answers.setProperty("material", getElementPositionInOptions(com.lastBmlContent, Item.getMaterialString(material)));
        answer();
        
        answer();

        Item item = PriceList.getPriceListFromBuyer(buyer).iterator().next().getItem();

        assertEquals(material, item.getMaterial());
    }

    @Test
    void testMaterialTypeCorrect_Meat() throws PriceList.NoPriceListOnBuyer {
        testMaterialTypeCorrect(factory.getIsMeatId(), ItemMaterials.MATERIAL_MEAT_DRAGON, false);
    }

    @Test
    void testMaterialTypeCorrect_Metal() throws PriceList.NoPriceListOnBuyer {
        testMaterialTypeCorrect(factory.getIsMetalId(), ItemMaterials.MATERIAL_BRASS, false);
    }

    @Test
    void testMaterialTypeCorrect_Wood() throws PriceList.NoPriceListOnBuyer {
        testMaterialTypeCorrect(factory.getIsWoodId(), ItemMaterials.MATERIAL_WOOD_OAK, false);
    }

    @Test
    void testMaterialTypeCorrect_Custom() throws PriceList.NoPriceListOnBuyer {
        testMaterialTypeCorrect(factory.getIsDefaultMaterialId(), ItemMaterials.MATERIAL_MEAT_DRAGON, true);
    }

    @Test
    void testMaterialTypeCorrect_Any() throws PriceList.NoPriceListOnBuyer {
        testMaterialTypeCorrect(factory.getIsWoodId(), (byte)0, false);
    }

    @Test
    void testBackFromFinalQuestionDiffersWhenCustomMaterial() throws NoSuchQuestionException {
        int hatchet = 7;
        askQuestion();
        String templateIndex = getElementPositionInOptions(com.lastBmlContent, hatchet);
        answers.setProperty("templateId", templateIndex);
        answer();
        
        answers.setProperty("list_all_materials", "true");
        answer();
        
        answers.setProperty("material", "0");
        answer();
        
        answers.setProperty("back", "true");
        answer();

        Creature newOwner = factory.createNewPlayer();
        FakeCommunicator newCom = factory.getCommunicator(newOwner);
        Creature newBuyer = factory.createNewBuyer(newOwner);
        Properties newAnswers;
        AddItemToBuyerQuestion newQuestion = new AddItemToBuyerQuestion(newOwner, newBuyer.getWurmId());
        newQuestion.sendQuestion();

        newAnswers = new Properties();
        newAnswers.setProperty("templateId", templateIndex);
        newQuestion.answer(newAnswers);
        newQuestion = (AddItemToBuyerQuestion)Questions.getQuestion(getPassThroughId(newCom.lastBmlContent));

        newAnswers = new Properties();
        newAnswers.setProperty("material", "0");
        newQuestion.answer(newAnswers);
        newQuestion = (AddItemToBuyerQuestion)Questions.getQuestion(getPassThroughId(newCom.lastBmlContent));

        newAnswers = new Properties();
        newAnswers.setProperty("back", "true");
        newQuestion.answer(newAnswers);

        String[] ownerBml = factory.getCommunicator(sender).getBml();
        String[] newOwnerBml = factory.getCommunicator(newOwner).getBml();
        assertNotEquals(removePassThroughAndDefault(ownerBml[4]), removePassThroughAndDefault(newOwnerBml[3]));
        assertEquals(removePassThroughAndDefault(ownerBml[2]), removePassThroughAndDefault(ownerBml[4]));
        assertEquals(removePassThroughAndDefault(newOwnerBml[1]), removePassThroughAndDefault(newOwnerBml[3]));
    }

    @Test
    void testBackAndNextWithoutChangingCustomMaterial() {
        int hatchet = 7;
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, hatchet));
        answer();
        
        answers.setProperty("list_all_materials", "true");
        answer();
        
        answers.setProperty("material", getElementPositionInOptions(com.lastBmlContent, ItemMaterials.IRON_MATERIAL_STRING));
        answer();
        
        answers.setProperty("back", "true");
        answer();

        String[] ownerBml = com.getBml();
        assertEquals(removePassThroughAndDefault(ownerBml[2]), removePassThroughAndDefault(ownerBml[4]));
    }

    @Test
    void testBackAndNextWithoutChangingIsMetal() {
        int hatchet = 7;
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, hatchet));
        answer();
        
        answers.setProperty("material", getElementPositionInOptions(com.lastBmlContent, ItemMaterials.IRON_MATERIAL_STRING));
        answer();
        
        answers.setProperty("back", "true");
        answer();

        String[] ownerBml = factory.getCommunicator(sender).getBml();
        assertEquals(removePassThroughAndDefault(ownerBml[1]), removePassThroughAndDefault(ownerBml[3]));
    }

    @Test
    void testBackAndNextWithoutChangingIsWood() {
        int marketStall = 580;
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, marketStall));
        answer();
        
        answers.setProperty("material", getElementPositionInOptions(com.lastBmlContent, ItemMaterials.WOOD_BIRCH_MATERIAL_STRING));
        answer();
        
        answers.setProperty("back", "true");
        answer();

        String[] ownerBml = factory.getCommunicator(sender).getBml();
        assertEquals(removePassThroughAndDefault(ownerBml[1]), removePassThroughAndDefault(ownerBml[3]));
    }

    @Test
    void testBackAndNextWithChangingCustomMaterial() {
        int hatchet = 7;
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, hatchet));
        answer();
        
        answers.setProperty("list_all_materials", "true");
        answer();
        
        answers.setProperty("material", getElementPositionInOptions(com.lastBmlContent, ItemMaterials.MEAT_DRAGON_MATERIAL_STRING));
        answer();
        
        answers.setProperty("back", "true");
        answer();
        
        answers.setProperty("material", getElementPositionInOptions(com.lastBmlContent, ItemMaterials.MEAT_DRAGON_MATERIAL_STRING));
        answer();
        
        answers.setProperty("back", "true");
        answer();

        String[] ownerBml = com.getBml();
        assertEquals(removePassThroughAndDefault(ownerBml[4]), removePassThroughAndDefault(ownerBml[6]));
    }

    @Test
    void testBackAndNextWithChangingCustomMaterialWithFilter() {
        int hatchet = 7;
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, hatchet));
        answer();
        
        answers.setProperty("list_all_materials", "true");
        answer();
        
        answers.setProperty("material", getElementPositionInOptions(com.lastBmlContent, ItemMaterials.MEAT_DRAGON_MATERIAL_STRING));
        answer();
        
        answers.setProperty("back", "true");
        answer();

        filterAndAnswer("*dragon*");
        
        answers.setProperty("material", getElementPositionInOptions(com.lastBmlContent, ItemMaterials.MEAT_DRAGON_MATERIAL_STRING));
        answer();
        
        answers.setProperty("back", "true");
        answer();
        filterAndAnswer("*dragon*");

        String[] ownerBml = com.getBml();
        assertEquals(removePassThroughAndDefault(ownerBml[5]), removePassThroughAndDefault(ownerBml[8]));
    }

    @Test
    void testSingleMaterialItemSettingMaterialCorrectly() throws PriceList.NoPriceListOnBuyer, NoSuchTemplateException {
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, backPack));
        answer();
        answers.setProperty("material", "0");
        answer();
        answer();

        assertEquals(ItemTemplateFactory.getInstance().getTemplate(backPack).getMaterial(), PriceList.getPriceListFromBuyer(buyer).iterator().next().getItem().getMaterial());
    }

    @Test
    void testAddItemLeadsBackToSetBuyerPrices() throws NoSuchQuestionException {
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, backPack));
        answer();
        answers.setProperty("material", "0");
        answer();
        answer();

        Question question = Questions.getQuestion(getPassThroughId(com.lastBmlContent));
        assertTrue(question instanceof SetBuyerPricesQuestion);
    }

    @Test
    void testPlayerCannotAddToList() {
        Question question = new AddItemToBuyerQuestion(player, buyer.getWurmId());
        FakeCommunicator com = factory.getCommunicator(player);
        question.sendQuestion();
        Properties answers = new Properties();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, backPack));
        question.answer(answers);

        assertThat(player, receivedMessageContaining("don't own that"));
    }

    @Test
    void testUnknownReplacedWithAnyMaterial() throws NoSuchTemplateException {
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(factory.getIsUnknownMaterial());
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, template.getTemplateId()));
        answer();

        Matcher matcher = allOptions.matcher(com.lastBmlContent);
        assertTrue(matcher.find());

        assertEquals("Any material", matcher.group(1));
    }

    @Test
    void testKilogramsString() throws NoSuchTemplateException {
        Pattern weight = Pattern.compile("([0-9.]+kg)");

        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(factory.getIsWoodId());
        assert template.getWeightGrams() == 24000;

        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, template.getTemplateId()));
        answer();
        answers.setProperty("material", "0");
        answer();

        Matcher matcher = weight.matcher(com.lastBmlContent);
        assertTrue(matcher.find());
        assertEquals("24kg", matcher.group(1));

        template = ItemTemplateFactory.getInstance().getTemplate(factory.getIsCoinId());
        assert template.getWeightGrams() == 10;
        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, factory.getIsCoinId()));
        answer();
        answers.setProperty("material", "0");
        answer();

        matcher = weight.matcher(com.lastBmlContent);
        assertTrue(matcher.find());
        assertEquals("0.01kg", matcher.group(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testAllTemplateMaterialCombinations() throws NoSuchFieldException, IllegalAccessException {
        AddItemToBuyerQuestion question = new AddItemToBuyerQuestion(owner, buyer.getWurmId());
        question.sendQuestion();
        Field itemTemplates = AddItemToBuyerQuestion.class.getDeclaredField("itemTemplates");
        itemTemplates.setAccessible(true);
        List<ItemTemplate> templates = (List<ItemTemplate>)itemTemplates.get(question);
        assert templates.size() > 0;

        int counter = 0;
        for (ItemTemplate template : templates) {
            if (template == null)
                continue;
            for (byte material = 0; material < ItemMaterials.MATERIAL_MAX; ++material) {
                TempItem item = assertDoesNotThrow(() -> new TempItem(ItemFactory.generateName(template, (byte)0), template, 1.0f, "PriceList"));
                byte finalMaterial = material;
                assertDoesNotThrow(() -> item.setMaterial(finalMaterial));
                ++counter;
            }
        }

        System.out.println(counter + " combinations processed.");
    }
}
