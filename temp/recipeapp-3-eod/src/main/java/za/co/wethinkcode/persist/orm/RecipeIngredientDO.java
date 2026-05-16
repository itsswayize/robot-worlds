/*
 * Copyright 2021 Mike Morris <mikro2nd@gmail.com>. All rights reserved.
 */
package za.co.wethinkcode.persist.orm;

import net.lemnik.eodsql.ResultColumn;

/**
 * TODO: javadoc RecipeIngredientDO
 */
public class RecipeIngredientDO
{
    @ResultColumn( value = "pId" )
    public int productId;

    @ResultColumn( value = "pName" )
    public String productName;

    @ResultColumn( value = "iId" )
    public int ingredientId;

    @ResultColumn( value = "iName" )
    public String ingredientName;

    @ResultColumn( value = "iType" )
    public String ingredientType;

    public int qty;

    public String units;

    public RecipeIngredientDO(){}
}
