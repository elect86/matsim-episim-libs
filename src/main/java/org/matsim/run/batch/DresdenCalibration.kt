package org.matsim.run.batch

import com.google.inject.AbstractModule
import org.matsim.core.config.Config
import org.matsim.run.modules.SnzDresdenScenario.config
import org.matsim.episim.BatchRun
import org.matsim.run.modules.SnzDresdenScenario
import org.matsim.episim.EpisimConfigGroup
import org.matsim.core.config.ConfigUtils
import org.matsim.episim.BatchRun.GenerateSeeds
import kotlin.jvm.JvmStatic
import org.matsim.run.RunParallel
import org.matsim.run.batch.DresdenCalibration
import org.matsim.run.modules.SnzDresdenScenario_test

/**
 * Calibration for Dresden scenario
 */
class DresdenCalibration : BatchRun<DresdenCalibration.Params?> {

    override fun getBindings(id: Int, params: Params?): AbstractModule? {
        return SnzDresdenScenario_test.Builder().apply {
            scale(params == null ? 1.0 : params.scale)
            .setActivityHandling(EpisimConfigGroup.ActivityHandling.startOfDay)
//				.setLeisureOffset( params == null ? 0d : params.leisureOffset)
//				.setLeisureNightly(leisureNightly)
//				.setLeisureNightlyScale(leisureNightlyScale)
                .build()
        }
    }

    override fun getMetadata(): BatchRun.Metadata {
        return BatchRun.Metadata.of("dresden", "calibration")
    }

    //	@Override
    //	public int getOffset() {
    //		return 10000;
    //	}
    override fun prepareConfig(id: Int, params: Params): Config? {
        val module = SnzDresdenScenario()
        val config = module.config()
        config.global().randomSeed = params.seed
        val episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup::class.java)
        episimConfig.calibrationParameter = episimConfig.calibrationParameter * params.thetaFactor
        episimConfig.calibrationParameter = episimConfig.calibrationParameter * params.OMI_inf

        //episimConfig.setHospitalFactor(); TODO
        return config
    }

    class Params {
        @GenerateSeeds(1)
        var seed: Long = 0

        @BatchRun.Parameter(0.8, 0.9)
        var thetaFactor = 0.0

        @BatchRun.Parameter(3, 3.5, 4, 4.5, 5, 5.5)
        var OMI_inf = 0.0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val args2 = arrayOf(
                    RunParallel.OPTION_SETUP, DresdenCalibration::class.java.name,
                    RunParallel.OPTION_PARAMS, Params::class.java.name,
                    RunParallel.OPTION_TASKS, Integer.toString(1),
                    RunParallel.OPTION_ITERATIONS, Integer.toString(5),
                    RunParallel.OPTION_METADATA
            )
            RunParallel.main(args2)
        }
    }
}
