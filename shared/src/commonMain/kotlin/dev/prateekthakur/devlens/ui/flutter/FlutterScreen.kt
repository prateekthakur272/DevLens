package dev.prateekthakur.devlens.ui.flutter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.prateekthakur.devlens.core.io.FilePicker
import dev.prateekthakur.devlens.domain.android.AndroidProjectAnalyzer
import dev.prateekthakur.devlens.domain.android.AndroidProjectInfo
import dev.prateekthakur.devlens.domain.build.BuildDiagnosis
import dev.prateekthakur.devlens.domain.build.BuildDoctor
import dev.prateekthakur.devlens.domain.build.DiagnosisConfidence
import dev.prateekthakur.devlens.domain.dependency.DependencyAnalyzer
import dev.prateekthakur.devlens.domain.dependency.DependencyNode
import dev.prateekthakur.devlens.domain.flutter.FlutterProjectAnalyzer
import dev.prateekthakur.devlens.domain.flutter.FlutterProjectInfo
import dev.prateekthakur.devlens.ui.common.EmptyState
import dev.prateekthakur.devlens.ui.common.ErrorBanner
import dev.prateekthakur.devlens.ui.common.LabeledValue
import dev.prateekthakur.devlens.ui.common.ScreenHeader
import dev.prateekthakur.devlens.ui.common.SectionCard
import dev.prateekthakur.devlens.ui.common.Tag
import dev.prateekthakur.devlens.ui.theme.Spacing
import kotlinx.coroutines.launch

private val tabs = listOf("Project", "Dependencies", "Build Doctor")

/** PRD §27-30 — Flutter project analysis, dependency graph and the Build Doctor. */
@Composable
fun FlutterScreen(
    flutterProjectAnalyzer: FlutterProjectAnalyzer,
    androidProjectAnalyzer: AndroidProjectAnalyzer,
    dependencyAnalyzer: DependencyAnalyzer,
    buildDoctor: BuildDoctor,
) {
    var projectDir by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var flutterInfo by remember { mutableStateOf<FlutterProjectInfo?>(null) }
    var androidInfo by remember { mutableStateOf<AndroidProjectInfo?>(null) }
    var dependencyTree by remember { mutableStateOf<List<DependencyNode>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun analyze() {
        error = null
        scope.launch {
            flutterProjectAnalyzer.analyze(projectDir).onSuccess { flutterInfo = it }.onFailure { error = it.message }
            androidProjectAnalyzer.analyze(projectDir).onSuccess { androidInfo = it }
            dependencyAnalyzer.analyze(projectDir).onSuccess { dependencyTree = it }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("Flutter", "Project, dependency graph and build-failure diagnosis")

        SectionCard(modifier = Modifier.padding(top = Spacing.lg)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(projectDir, { projectDir = it }, label = { Text("Project directory") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { scope.launch { FilePicker.pickDirectory("Open Flutter Project")?.let { projectDir = it } } }, modifier = Modifier.padding(start = Spacing.sm)) {
                    Text("Browse")
                }
                Button(onClick = { analyze() }, modifier = Modifier.padding(start = Spacing.sm)) { Text("Analyze") }
            }
        }
        error?.let { ErrorBanner(it) }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = Spacing.lg),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(top = Spacing.lg)) {
            when (selectedTab) {
                0 -> ProjectTab(flutterInfo, androidInfo)
                1 -> DependenciesTab(dependencyTree)
                2 -> BuildDoctorTab(buildDoctor)
            }
        }
    }
}

@Composable
private fun ProjectTab(flutter: FlutterProjectInfo?, android: AndroidProjectInfo?) {
    LazyColumn {
        item {
            SectionCard(title = "Toolchain") {
                LabeledValue("Flutter", flutter?.flutterVersion ?: "unknown")
                LabeledValue("Dart", flutter?.dartVersion ?: "unknown")
                if (android != null) {
                    LabeledValue("Gradle", android.gradleVersion ?: "unknown")
                    LabeledValue("AGP", android.agpVersion ?: "unknown")
                    LabeledValue("Kotlin", android.kotlinVersion ?: "unknown")
                    LabeledValue("compileSdk / targetSdk / minSdk", "${android.compileSdk} / ${android.targetSdk} / ${android.minSdk}")
                }
            }
        }
        item {
            SectionCard(title = "Dependencies (${flutter?.dependencies?.size ?: 0})", modifier = Modifier.padding(top = Spacing.lg)) {
                if (flutter?.dependencies.isNullOrEmpty()) EmptyState("No pubspec.yaml analyzed yet.")
                flutter?.dependencies.orEmpty().forEach { dep ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        if (!dep.isDirect) {
                            Tag("dev", MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = Spacing.sm))
                        }
                        Text(
                            "${dep.name}  ${dep.versionConstraint}${dep.resolvedVersion?.let { " → $it" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DependenciesTab(tree: List<DependencyNode>) {
    SectionCard {
        if (tree.isEmpty()) {
            EmptyState("No dependency graph yet — requires the `flutter` CLI on PATH.")
            return@SectionCard
        }
        flatten(tree).forEach { (depth, node) ->
            Text("${"    ".repeat(depth)}${node.name}  ${node.version}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 1.dp))
        }
    }
}

private fun flatten(nodes: List<DependencyNode>, depth: Int = 0): List<Pair<Int, DependencyNode>> =
    nodes.flatMap { listOf(depth to it) + flatten(it.children, depth + 1) }

@Composable
private fun BuildDoctorTab(buildDoctor: BuildDoctor) {
    var buildOutput by remember { mutableStateOf("") }
    var diagnoses by remember { mutableStateOf<List<BuildDiagnosis>>(emptyList()) }

    Column {
        SectionCard {
            Text("Paste Gradle/Flutter build output to diagnose failures.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                buildOutput,
                { buildOutput = it },
                modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = Spacing.md),
                label = { Text("Build output") },
            )
            Button(onClick = { diagnoses = buildDoctor.diagnose(buildOutput) }, modifier = Modifier.padding(top = Spacing.md)) {
                Text("Diagnose")
            }
        }
        if (buildOutput.isNotBlank() && diagnoses.isEmpty()) {
            EmptyState("No known failure pattern matched.")
        }
        LazyColumn(modifier = Modifier.padding(top = Spacing.lg)) {
            items(diagnoses) { diagnosis ->
                SectionCard(modifier = Modifier.padding(bottom = Spacing.md)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Tag(confidenceLabel(diagnosis.confidence), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(end = Spacing.sm))
                        Text(diagnosis.rootProblem, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text("Evidence: ${diagnosis.evidence}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.sm))
                    Text("Action: ${diagnosis.recommendedAction}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun confidenceLabel(confidence: DiagnosisConfidence) = when (confidence) {
    DiagnosisConfidence.HIGH -> "High confidence"
    DiagnosisConfidence.MEDIUM -> "Medium confidence"
    DiagnosisConfidence.LOW -> "Low confidence"
}
