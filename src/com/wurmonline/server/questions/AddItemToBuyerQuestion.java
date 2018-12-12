package com.wurmonline.server.questions;

import com.google.common.base.Joiner;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.MethodsItems;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.items.*;
import com.wurmonline.server.players.PlayerInfoFactory;
import com.wurmonline.shared.constants.ItemMaterials;
import com.wurmonline.shared.util.MaterialUtilities;
import mod.wurmunlimited.buyermerchant.PriceList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;

public class AddItemToBuyerQuestion extends QuestionExtension {
    private int stage = 0;
    private ItemTemplate itemTemplate;
    private byte material = 0;
    private boolean usingCustomMaterial = false;
    private String filter = "*";
    private ArrayList<ItemTemplate> itemTemplates = new ArrayList<>();
    private List<String> materialsList;
    static final byte[] allMeat = new byte[] {
        ItemMaterials.MATERIAL_FLESH,
        ItemMaterials.MATERIAL_MEAT_BEAR,
        ItemMaterials.MATERIAL_MEAT_BEEF,
        ItemMaterials.MATERIAL_MEAT_CANINE,
        ItemMaterials.MATERIAL_MEAT_CAT,
        ItemMaterials.MATERIAL_MEAT_DRAGON,
        ItemMaterials.MATERIAL_MEAT_FOWL,
        ItemMaterials.MATERIAL_MEAT_GAME,
        ItemMaterials.MATERIAL_MEAT_HORSE,
        ItemMaterials.MATERIAL_MEAT_HUMAN,
        ItemMaterials.MATERIAL_MEAT_HUMANOID,
        ItemMaterials.MATERIAL_MEAT_INSECT,
        ItemMaterials.MATERIAL_MEAT_LAMB,
        ItemMaterials.MATERIAL_MEAT_PORK,
        ItemMaterials.MATERIAL_MEAT_SEAFOOD,
        ItemMaterials.MATERIAL_MEAT_SNAKE,
        ItemMaterials.MATERIAL_MEAT_TOUGH
    };

    // Note - target is the buyer.
    AddItemToBuyerQuestion(Creature aResponder, long aTarget) {
        super(aResponder, "Add Item To Buyer", "Add a new item to the buyers list:", QuestionTypes.PRICEMANAGE, aTarget);
    }

    private AddItemToBuyerQuestion(Creature aResponder, long aTarget, ItemTemplate template, byte material, int stage, String filter, boolean customMaterial, List<String> materialList) {
        this(aResponder, aTarget);
        this.itemTemplate = template;
        this.material = material;
        this.stage = stage;
        this.filter = filter;
        this.usingCustomMaterial = customMaterial;
        this.materialsList = materialList;
    }

    @Override
    public void answer(Properties answers) {
        this.setAnswer(answers);
        Creature responder = this.getResponder();

        try {
            Creature buyer = Server.getInstance().getCreature(this.target);
            if (responder.getWurmId() != Economy.getEconomy().getShop(buyer).getOwnerId()) {
                responder.getCommunicator().sendNormalServerMessage("You don't own that buyer.");
                return;
            }
        } catch (NoSuchCreatureException | NoSuchPlayerException e) {
            responder.getCommunicator().sendNormalServerMessage("No such creature.");
            e.printStackTrace();
            return;
        }

        if (wasSelected("cancel")) {
            responder.getCommunicator().sendNormalServerMessage("You decide not to add anything.");
            return;
        }
        if (wasSelected("filterme")) {
            String filterText = answers.getProperty("filtertext");
            if (filterText == null || filterText.length() == 0) {
                this.filter = "*";
            } else {
                this.filter = filterText;
            }
        } else if (wasSelected("back")) {
            switch (stage) {
                case 1:
                case 2:
                    stage = 0;
                    break;
                case 3:
                    if (usingCustomMaterial)
                        stage = 2;
                    else
                        stage = 1;
                    break;
            }
            filter = "*";
        } else if (wasSelected("list_all_materials")) {
            usingCustomMaterial = true;
        } else {
            // Normal question flow.
            switch (stage) {
                case 0:
                    String tempId = answers.getProperty("templateId");
                    if (tempId != null && tempId.length() > 0) {
                        ItemTemplate template = getTemplate(Integer.parseInt(tempId));
                        if (template == null) {
                            responder.getCommunicator().sendNormalServerMessage("You decide not to add anything.");
                            return;
                        } else {
                            itemTemplate = template;
                            stage = 1;
                            filter = "*";
                        }
                    }
                    break;
                case 1:
                case 2:
                    String materialString = answers.getProperty("material");
                    if (materialString != null && materialString.length() > 0) {
                        try {
                            material = getMaterialFromListIndex(Integer.parseInt(materialString));
                            stage = 3;
                        } catch (NumberFormatException var52) {
                            logger.log(Level.WARNING, "Material was " + materialString);
                        }
                    }
                    break;
                case 3:
                    addItem();
                    new SetBuyerPricesQuestion(this.getResponder(), this.target).sendQuestion();
                    return;
            }
        }

        AddItemToBuyerQuestion question = new AddItemToBuyerQuestion(this.getResponder(), this.target, itemTemplate, material, stage, filter, stage < 1 ? false : usingCustomMaterial, materialsList);
        question.sendQuestion();
    }

    private void addItem() {
        Creature responder = this.getResponder();
        try {
            Creature buyer = Server.getInstance().getCreature(this.target);
    
            try {
                PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
                // Any material is 0.  Client will not label 0 material (unknown).
                PriceList.Entry newItem = priceList.addItem(itemTemplate.getTemplateId(), material);
                SetBuyerPricesQuestion.setItemQLAndPrice(newItem, -1, this.getAnswer(), responder);
                priceList.savePriceList();
                responder.getCommunicator().sendNormalServerMessage(buyer.getName() + " adds the item to their list.");
            } catch (PriceList.NoPriceListOnBuyer noPriceListOnBuyer) {
                responder.getCommunicator().sendNormalServerMessage(PriceList.noPriceListFoundPlayerMessage);
                noPriceListOnBuyer.printStackTrace();
            } catch (PriceList.PriceListFullException e) {
                responder.getCommunicator().sendNormalServerMessage(PriceList.noSpaceOnPriceListPlayerMessage);
            } catch (NoSuchTemplateException | IOException e) {
                responder.getCommunicator().sendNormalServerMessage(PriceList.couldNotCreateItemPlayerMessage);
                e.printStackTrace();
            }

        } catch (NoSuchPlayerException | NoSuchCreatureException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendQuestion() {
        switch (stage) {
            case 0:
                sendItemTypeQuestion();
                break;
            case 1:
            case 2:
                sendMaterialTypeQuestion();
                break;
            case 3:
                sendQLPriceQuestion();
                break;
        }
    }

    private String getTemplateString(@Nullable ItemTemplate template) {
        if (template == null)
            return "Nothing";
        return getTemplateString(template, template.getMaterial());
    }

    private String getTemplateString(ItemTemplate template, byte material) {
        if (!template.isMetal() && !template.isWood() && !template.isOre && !template.isShard) {
            if (template.bowUnstringed) {
                return template.getName() + " - " + template.sizeString + " [unstringed]";
            } else {
                return template.getName() + (template.sizeString.isEmpty() ? "" : " - " + template.sizeString);
            }
        } else {
            return template.getName() + " - " + template.sizeString + Item.getMaterialString(material) + " ";
        }
    }

    private ItemTemplate getTemplate(int aTemplateId) {
        return this.itemTemplates.get(aTemplateId);
    }

    private void sendItemTypeQuestion() {
        ItemTemplate[] templates = ItemTemplateFactory.getInstance().getTemplates();
        Arrays.sort(templates, Comparator.comparing(t -> t.getName().toLowerCase()));

        for (ItemTemplate template : templates) {
            if (!template.isNoCreate() && !template.unique && !template.isRoyal && !template.isUnstableRift() && PlayerInfoFactory.wildCardMatch(template.getName().toLowerCase(), this.filter.toLowerCase())) {
                this.itemTemplates.add(template);
            }
        }

        // Needed when filter matches none.
        if (this.itemTemplates.size() != 1) {
            this.itemTemplates.add(0, null);
        }

        StringBuilder buf = new StringBuilder(this.getBmlHeader());
        buf.append("text{text=\"You can change the material type on the next screen\"}");
        buf.append("text{text=\"Remember - Just because you can add an item to the list doesn't mean anybody will be able to sell you one.\"}");
        buf.append("text{text=\"\"}");
        buf.append("harray{label{text=\"Item\"};dropdown{id=\"templateId\";options=\"");

        String defaultId = "0";
        for(int x = 0; x < this.itemTemplates.size(); ++x) {
            if (x > 0) {
                buf.append(",");
            }
            ItemTemplate tp = this.itemTemplates.get(x);
            if (itemTemplate != null && tp == itemTemplate)
                defaultId = Integer.toString(x);
            buf.append(getTemplateString(tp));
        }
        buf.append("\";default=\"" + defaultId + "\";}};");
        buf.append("text{text=\"\"}");
        buf.append("text{text=\"* is a wildcard that stands in for one or more characters.\ne.g. *clay* to find all clay items or lump* to find all types of lump.\"}");
        buf.append("harray{button{text=\"Filter list\";id=\"filterme\"};label{text=\" using \"};input{maxchars=\"30\";id=\"filtertext\";text=\"" + this.filter + "\";onenter=\"filterme\"}}");
        buf.append("text{text=\"\"}");
        buf.append("harray {button{text=\"Next\";id=\"submit\"};label{text=\" \";id=\"spacedlxg\"};button{text=\"Cancel\";id=\"cancel\"}}}};null;null;}");
        this.getResponder().getCommunicator().sendBml(400, 300, true, true, buf.toString(), 200, 200, 200, this.title);
    }

    private byte getMaterialFromListIndex(int idx) {
        if (idx == 0) {
            if (materialsList.size() == 1 && !materialsList.get(0).equals("Any material"))
                return itemTemplate.getMaterial();
            else
                return (byte)0;
        }
        return Materials.convertMaterialStringIntoByte(materialsList.get(idx - 1));
    }

    private String appendMaterialTypes(StringBuilder buf) {
        materialsList = new ArrayList<>(ItemMaterials.MATERIAL_MAX);

        if (usingCustomMaterial) {
            for (int x = 1; x <= ItemMaterials.MATERIAL_MAX; ++x) {
                String materialString = Item.getMaterialString((byte)x);
                if (!materialString.equals("unknown") && PlayerInfoFactory.wildCardMatch(materialString.toLowerCase(), this.filter.toLowerCase())) {
                    materialsList.add(materialString);
                }
            }
        } else if (itemTemplate.isWood()) {
            for (byte mat : MethodsItems.getAllNormalWoodTypes()) {
                String materialString = Item.getMaterialString(mat);
                if (!materialString.equals("unknown") && PlayerInfoFactory.wildCardMatch(materialString.toLowerCase(), this.filter.toLowerCase())) {
                    materialsList.add(materialString);
                }
            }
        } else if (itemTemplate.isMetal()) {
            // Ignore scrap, ore, lump, altar as their metal types are already set on the template.
            if (!itemTemplate.getName().equals("ore") && !itemTemplate.getName().equals("scrap") && !itemTemplate.getName().equals("lump") && !itemTemplate.getName().equals("altar")) {
                for (byte mat : MethodsItems.getAllMetalTypes()) {
                    String materialString = Item.getMaterialString(mat);
                    if (!materialString.equals("unknown") && PlayerInfoFactory.wildCardMatch(materialString.toLowerCase(), this.filter.toLowerCase())) {
                        materialsList.add(materialString);
                    }
                }
            }
        } else if (itemTemplate.isMeat()) {
            for (byte mat : allMeat) {
                String materialString = Item.getMaterialString(mat);
                if (!materialString.equals("unknown") && PlayerInfoFactory.wildCardMatch(materialString.toLowerCase(), this.filter.toLowerCase())) {
                    materialsList.add(materialString);
                }
            }
        }

        String defaultMaterialString = Item.getMaterialString(itemTemplate.getMaterial());
        int id = 0;

        if (materialsList.size() == 0) {
            if (!defaultMaterialString.equals("unknown")) {
                materialsList.add(defaultMaterialString);
                buf.append(defaultMaterialString);
            } else {
                materialsList.add("Any material");
                buf.append("Any material");
            }
        }
        else {
            buf.append("Any material,");
            buf.append(Joiner.on(",").join(materialsList));
            id = materialsList.indexOf(defaultMaterialString);
            if (id != -1)
                // Plus one for Any.
                ++id;
            else
                id = 0;
        }

        return Integer.toString(id);
    }

    private void sendMaterialTypeQuestion() {
        assert itemTemplate != null;
        StringBuilder buf = new StringBuilder(this.getBmlHeader());
        buf.append("text{text=\"You can change the material type if necessary (e.g. wooden items to specific wood type)\"}");
        buf.append("text{text=\"\"}");
        if (usingCustomMaterial)
            buf.append("text{type=\"bold\";text=\"Be aware, it is possible to select materials that aren't possible to make.\"}");
        else
            buf.append("text{text=\"If the material you want is not in the dropdown you can list all materials.  You should almost never need to.\"}");
        buf.append("text{text=\"\"}");
        buf.append("label{text=\"Item:  ");
        buf.append(getTemplateString(itemTemplate, (material != 0) ? material : itemTemplate.getMaterial()));
        buf.append("\"}");

        buf.append("harray{label{text=\"Material:  \"};dropdown{id=\"material\";options=\"");
        String defaultId = appendMaterialTypes(buf);
        buf.append("\";default=\"" + defaultId + "\";}};");
        buf.append("text{text=\"\"}");

        buf.append("text{text=\"* is a wildcard that stands in for one or more characters.\ne.g. *wood to find all types of wood.\"}");
        buf.append("harray{button{text=\"Filter list\";id=\"filterme\"};label{text=\" using \"};input{maxchars=\"30\";id=\"filtertext\";text=\"" + this.filter + "\";onenter=\"filterme\"};label{text=\" \";id=\"spacedlxg\"};button{text=\"List All Materials\";id=\"list_all_materials\"};}");
        buf.append("text{text=\"\"}");
        buf.append("harray {button{text=\"Next\";id=\"submit\"};label{text=\" \";id=\"spacedlxg\"};button{text=\"Back\";id=\"back\"};label{text=\" \";id=\"spacedlxg\"};button{text=\"Cancel\";id=\"cancel\"};}}}null;null;};");
        this.getResponder().getCommunicator().sendBml(400, 300, true, true, buf.toString(), 200, 200, 200, this.title);
    }

    private void sendQLPriceQuestion() {
        StringBuilder buf = new StringBuilder(this.getBmlHeader());
        DecimalFormat df = new DecimalFormat("#.##");
        buf.append("table{rows=\"1\"; cols=\"8\";label{text=\"Item type\"};label{text=\"Material\"};label{text=\"Weight\"};label{text=\"Min. QL\"};label{text=\"Gold\"};label{text=\"Silver\"};label{text=\"Copper\"};label{text=\"Iron\"}");

        // New item row
        buf.append("harray{label{text=\"" + itemTemplate.getName() + "\"}};");
        buf.append("harray{label{text=\"" + (material == 0 ? "Any" : MaterialUtilities.getMaterialString(material)) + "\"}};");
        buf.append("harray{label{text=\"" + df.format(itemTemplate.getWeightGrams() / 1000.0f) + "kg\"}};");
        buf.append("harray{input{maxchars=\"3\"; id=\"q\";text=\"" + df.format(1) + "\"};label{text=\" \"}};");
        buf.append("harray{input{maxchars=\"3\"; id=\"g\";text=\"0\"};label{text=\" \"}};");
        buf.append("harray{input{maxchars=\"2\"; id=\"s\";text=\"0\"};label{text=\" \"}};");
        buf.append("harray{input{maxchars=\"2\"; id=\"c\";text=\"0\"};label{text=\" \"}};");
        buf.append("harray{input{maxchars=\"2\"; id=\"i\";text=\"0\"};label{text=\" \"}}");

        buf.append("}");
        buf.append("text{text=\"\"}");
        buf.append("harray {button{text=\"Add Item\";id=\"submit\"};label{text=\" \";id=\"spacedlxg\"};button{text=\"Back\";id=\"back\"};label{text=\" \";id=\"spacedlxg\"};button{text=\"Cancel\";id=\"cancel\"};}}}null;null;};");
        this.getResponder().getCommunicator().sendBml(400, 300, true, true, buf.toString(), 200, 200, 200, this.title);
    }
}
