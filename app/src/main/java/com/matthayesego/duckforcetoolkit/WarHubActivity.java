package com.matthayesego.duckforcetoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Compatibility entry point for older War Center shortcuts.
 *
 * The visible War landing page now lives inside TornFcaCurrentActivity so the bottom navigation
 * remains present. Ranked War and Territories still open their dedicated detail activities from
 * that persistent War tab.
 */
public class WarHubActivity extends Activity {
    @SuppressWarnings("unused") private static final String RANKED_LABEL="Ranked War";
    @SuppressWarnings("unused") private static final String TERRITORY_LABEL="Territories";
    @SuppressWarnings("unused") private static final Class<?> RANKED_SCREEN=WarCenterActivity.class;
    @SuppressWarnings("unused") private static final Class<?> TERRITORY_SCREEN=TerritoryWarActivity.class;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Intent i=new Intent(this,TornFcaCurrentActivity.class);
        i.putExtra(TornFcaCurrentActivity.EXTRA_SECTION,"War");
        copyScope(i);
        startActivity(i);
        finish();
    }

    private void copyScope(Intent i){
        Intent from=getIntent();
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_ID,from.getIntExtra(FactionOpsActivity.EXTRA_FACTION_ID,0));
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_NAME,from.getStringExtra(FactionOpsActivity.EXTRA_FACTION_NAME));
        i.putExtra(FactionOpsActivity.EXTRA_FACTION_API,from.getBooleanExtra(FactionOpsActivity.EXTRA_FACTION_API,false));
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_ID,from.getIntExtra(DeveloperConsoleActivity.EXTRA_FACTION_ID,0));
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_NAME,from.getStringExtra(DeveloperConsoleActivity.EXTRA_FACTION_NAME));
        i.putExtra(DeveloperConsoleActivity.EXTRA_FACTION_API,from.getBooleanExtra(DeveloperConsoleActivity.EXTRA_FACTION_API,false));
        i.putExtra(DeveloperConsoleActivity.EXTRA_POSITION,from.getStringExtra(DeveloperConsoleActivity.EXTRA_POSITION));
    }
}
