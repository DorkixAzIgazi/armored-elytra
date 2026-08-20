package dorkix.armored.elytra;

import net.fabricmc.api.ClientModInitializer;

import dorkix.armored.elytra.compat.DetailArmorBarCompat;

public class ArmoredElytraClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DetailArmorBarCompat.tryRegister();
	}
}