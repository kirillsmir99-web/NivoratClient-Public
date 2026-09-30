package activity.client.module.api;

import activity.client.module.impl.combat.ParticlePhysicsModule;
import activity.client.module.impl.combat.RaycastPredictorModule;
import activity.client.module.impl.combat.ShaderPassModule;
import activity.client.module.impl.combat.VectorStreamModule;
import activity.client.module.impl.combat.MatrixTransformModule;
import activity.client.module.impl.defense.LightmapFilterModule;
import activity.client.module.impl.defense.OcclusionCacheModule;
import activity.client.module.impl.defense.BufferPipelineModule;
import activity.client.module.impl.defense.ChunkBufferModule;
import activity.client.module.impl.utility.AudioWaveModule;
import activity.client.module.impl.utility.ModelMeshModule;
import activity.client.module.impl.utility.CartHudModule;
import activity.client.module.impl.utility.HPReaperModule;
import activity.client.module.impl.utility.SurfaceImpactModule;

public final class BuiltinModules {

    private BuiltinModules() {}

    public static void registerAll() {

        ModuleRegistry.register(new ParticlePhysicsModule());
        ModuleRegistry.register(new VectorStreamModule());
        ModuleRegistry.register(new ShaderPassModule());
        ModuleRegistry.register(new MatrixTransformModule());
        ModuleRegistry.register(new RaycastPredictorModule());

        ModuleRegistry.register(new BufferPipelineModule());
        ModuleRegistry.register(new OcclusionCacheModule());
        ModuleRegistry.register(new LightmapFilterModule());
        ModuleRegistry.register(new ChunkBufferModule());

        ModuleRegistry.register(new HPReaperModule());
        ModuleRegistry.register(new ModelMeshModule());
        ModuleRegistry.register(new AudioWaveModule());
        ModuleRegistry.register(new CartHudModule());
        ModuleRegistry.register(new activity.client.module.impl.utility.CooldownHudModule());
        ModuleRegistry.register(new SurfaceImpactModule());
    }
}
