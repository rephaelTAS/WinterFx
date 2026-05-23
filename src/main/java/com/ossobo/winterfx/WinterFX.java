package com.ossobo.winterfx;

import com.ossobo.winterfx.AlertSystem.SystemsAlerty;
import com.ossobo.winterfx.ImageManager.ImageService;
import com.ossobo.winterfx.bootstrap.NexusFXBootstrap;
import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.resources.api.ResourceAPI;
import com.ossobo.winterfx.userhelp.*;
import com.ossobo.winterfx.view.ViewManager;

import javafx.stage.Stage;

/**
 * 🎯 NEXUS FX - Fachada Principal do Framework v9.1
 *
 * Ponto único de acesso para todos os serviços do framework.
 *
 * ✅ v8.0.5: Adicionado módulo UserHelp
 *    - validator(): validação visual de campos de formulário
 *    - guard(): proteção de ações críticas com confirmação explícita
 *    - toast(): feedback não-bloqueante com ação opcional
 *    - onboarding(): tour guiado de descoberta
 *
 * ✅ v8.0.4: Arquitetura corrigida
 *    - ResourceAPI: registro e consulta de TODOS os recursos
 *    - ViewManager: carregamento de views FXML
 *    - ImageService: carregamento de imagens (conveniência)
 *    - AlertaSystem: sistema de alertas e confirmações
 *    - DialogOrchestrator: diálogos modais
 *    - DiContainer: injeção de dependência
 *
 * ❌ REMOVIDO: viewRegistry() - uso interno, acessar via ResourceAPI
 * ❌ REMOVIDO: images() → ImageRegistry - uso interno, acessar via ImageService
 *
 * @author Rafael Tavares
 * @since 10.0
 */
public final class WinterFX {

    static NexusFXBootstrap bootstrap;

    private WinterFX() {
        throw new UnsupportedOperationException("Classe utilitária - não instanciar");
    }

    // ================================================================
    // INICIALIZAÇÃO
    // ================================================================

    /**
     * ✅ Vincula o bootstrap (chamado internamente pelo NexusFXBootstrap)
     */
    public static void link(NexusFXBootstrap bootstrap) {
        WinterFX.bootstrap = bootstrap;
        if (bootstrap != null) {
            System.out.println("✅ NexusFX.link() - Bootstrap vinculado: "
                    + bootstrap.getClass().getSimpleName());
        }
    }

    /**
     * Retorna o bootstrap para inicialização tardia
     */
    public static NexusFXBootstrap getBootstrap() {
        return bootstrap;
    }

    // ================================================================
    // 🌐 RESOURCE API - PONTO CENTRAL DE TODOS OS RECURSOS
    // ================================================================

    /**
     * 🌐 ResourceAPI - Registro e consulta de TODOS os recursos
     *
     * Este é o datacenter central do framework. Tudo é registrado aqui:
     * views FXML, imagens, CSS, sons, alertas.
     *
     * <pre>
     * Uso:
     *   // Registro
     *   NexusFX.resources().register(viewDescriptor);
     *   NexusFX.resources().register(imageDescriptor);
     *
     *   // Consulta
     *   NexusFX.resources().getViewUrl("login");
     *   NexusFX.resources().getImageUrl("logo");
     *   NexusFX.resources().find("main");
     *   NexusFX.resources().exists("logo", ResourceType.IMAGE);
     *
     *   // Listagem
     *   NexusFX.resources().listAllIds();
     *   NexusFX.resources().listAllViews();
     *
     *   // Estatísticas
     *   NexusFX.resources().count();
     *   NexusFX.resources().countByType(ResourceType.IMAGE);
     * </pre>
     */
    public static ResourceAPI resources() {
        if (bootstrap == null) {
            throw new IllegalStateException(
                    "❌ NexusFX não foi inicializado! Chame NexusFXBootstrap.run() primeiro.");
        }
        return bootstrap.getResourceAPI();
    }

    // ================================================================
    // 🪟 VIEW MANAGER - CARREGAMENTO DE VIEWS FXML
    // ================================================================

    /**
     * 🪟 ViewManager - Carregamento de views FXML com cache
     *
     * <pre>
     * Uso:
     *   // Carregar view (com cache)
     *   LoadedView<?> view = NexusFX.views().loadView("main");
     *   Parent root = view.getRoot();
     *
     *   // Carregar view com tipo específico
     *   LoadedView<MainController> view = NexusFX.views().loadView("main", MainController.class);
     *
     *   // Carregar view fresh (sem cache, para diálogos)
     *   LoadedView<?> dialog = NexusFX.views().loadFreshView("form-dialog");
     *
     *   // Recarregar view
     *   NexusFX.views().reloadView("main", MainController.class);
     *
     *   // Limpar cache
     *   NexusFX.views().clearCache();
     * </pre>
     */
    public static ViewManager views() {
        if (bootstrap == null) {
            throw new IllegalStateException(
                    "❌ NexusFX não foi inicializado! Chame NexusFXBootstrap.run() primeiro.");
        }
        return bootstrap.getViewManager();
    }

    // ================================================================
    // 🖼️ IMAGE SERVICE - CARREGAMENTO DE IMAGENS (CONVENIÊNCIA)
    // ================================================================

    /**
     * 🖼️ ImageService - Carregamento de imagens (API de conveniência)
     *
     * Para registro de imagens, use: {@link #resources() resources().register(imageDescriptor)}
     * Para consulta de URLs, use: {@link #resources() resources().getImageUrl("id")}
     *
     * <pre>
     * Uso:
     *   // Carregar imagem em ImageView
     *   NexusFX.images().load(imageView, "logo");
     *
     *   // Carregar imagem com tamanho específico
     *   NexusFX.images().load(imageView, "logo", 200, 100);
     *
     *   // Carregar ícone para Stage
     *   NexusFX.images().load(stage, "app-icon");
     *
     *   // Carregar ícone para Label
     *   NexusFX.images().load(label, "menu-icon");
     *
     *   // Carregar imagem como objeto
     *   Image img = NexusFX.images().loadImage("logo");
     *
     *   // Pré-carregar imagens
     *   NexusFX.images().preloadImages("logo", "splash", "icon");
     *
     *   // Limpar cache
     *   NexusFX.images().clearImageCache();
     * </pre>
     */
    public static ImageService images() {
        if (bootstrap == null) {
            throw new IllegalStateException(
                    "❌ NexusFX não foi inicializado! Chame NexusFXBootstrap.run() primeiro.");
        }

        // ✅ Obtém do bootstrap (inicializado corretamente)
        ImageService service = bootstrap.getImageService();

        if (service == null) {
            // Fallback via DiContainer
            try {
                DiContainer di = di();
                if (di != null) {
                    service = di.getBean(ImageService.class);
                }
            } catch (Exception e) {
                // Fallback: cria nova instância standalone
                service = new ImageService();
            }
        }

        return service;
    }

    // ================================================================
// ⚠️ SYSTEMS ALERTY - SISTEMA DE ALERTAS (FACHADA PÚBLICA)
// ================================================================

    /**
     * ⚠️ SystemsAlerty - Sistema de alertas e confirmações (fachada pública)
     *
     * <pre>
     * Uso:
     *   // Alertas informativos
     *   NexusFX.alerts().info("Sucesso", "Operação concluída", "Sistema");
     *   NexusFX.alerts().warn("Atenção", "Verifique os dados", "Validação");
     *   NexusFX.alerts().erro("Erro", "Falha na operação", "Detalhes", "Sistema");
     *   NexusFX.alerts().critical("Crítico", "Sistema indisponível", "Sistema");
     *
     *   // Confirmações
     *   NexusFX.alerts().confirmar("Deseja continuar?", "Confirmação", resultado -> {
     *       if (resultado) { ... }
     *   });
     *   NexusFX.alerts().confirmarAviso(mensagem, detalhes, titulo, callback);
     *   NexusFX.alerts().confirmarPerigo(mensagem, detalhes, titulo, callback);
     *   NexusFX.alerts().confirmarSucesso(mensagem, detalhes, titulo, callback);
     *
     *   // Gerenciamento
     *   NexusFX.alerts().fecharTodosAlertas();
     *   NexusFX.alerts().getQuantidadeAlertasAtivos();
     * </pre>
     */
    public static SystemsAlerty alerts() {
        if (bootstrap == null) {
            throw new IllegalStateException(
                    "❌ NexusFX não foi inicializado! Chame NexusFXBootstrap.run() primeiro.");
        }
        return bootstrap.getAlertaSystem();
    }

    // ================================================================
// ✅ USER HELP - SISTEMA DE AJUDA AO USUÁRIO
// ================================================================

    /**
     * ✅ FieldValidator - Validação visual de campos de formulário
     *
     * <pre>
     * Uso:
     *   NexusFX.validator()
     *       .required(nomeField, "Nome")
     *       .email(emailField)
     *       .custom(codField, v -> !service.existe(v), "Código já existe")
     *       .onSubmit(() -> salvar());
     * </pre>
     */
    public static FieldValidator validator() {
        return new FieldValidator();
    }

    /**
     * ✅ ActionGuard - Proteção de ações críticas com confirmação explícita
     *
     * <pre>
     * Uso:
     *   NexusFX.guard()
     *       .title("Excluir Funcionário")
     *       .warning("João Silva será removido permanentemente")
     *       .impact("⚠ 23 movimentações ficarão sem responsável")
     *       .requireConfirmation("JOÃO SILVA")
     *       .onConfirm(() -> service.excluir(codDep))
     *       .show();
     * </pre>
     */
    public static ActionGuard guard() {
        return new ActionGuard();
    }

    /**
     * ✅ ActionResult - Toast de feedback não-bloqueante
     *
     * <pre>
     * Uso:
     *   NexusFX.toast()
     *       .success("Funcionário salvo com sucesso!")
     *       .action("Desfazer", () -> service.reverter(codDep))
     *       .duration(5)
     *       .show();
     * </pre>
     */
    public static ActionResult toast() {
        return new ActionResult();
    }

    /**
     * ✅ OnboardingHint - Tour guiado de descoberta da interface
     *
     * <pre>
     * Uso:
     *   NexusFX.onboarding()
     *       .step(novoButton, "Comece cadastrando um funcionário", Position.BOTTOM)
     *       .step(tabela, "Clique com botão direito para opções", Position.LEFT)
     *       .start();
     * </pre>
     */
    public static OnboardingHint onboarding() {
        return new OnboardingHint();
    }

    // ================================================================
    // 🪟 DIALOG ORCHESTRATOR - DIÁLOGOS MODAIS
    // ================================================================

    /**
     * 🪟 DialogOrchestrator - Diálogos modais
     *
     * <pre>
     * Uso:
     *   // Abrir modal simples
     *   NexusFX.dialogs().openModal("dialog-id", "Título", stage);
     *
     *   // Abrir modal com configurador
     *   NexusFX.dialogs().openModalWithController("form", "Editar", stage, controller -> {
     *       controller.setData(dados);
     *   });
     *
     *   // Abrir modal com retorno
     *   Optional<String> result = NexusFX.dialogs()
     *       .openModalWithControllerWithResult("form", "Editar", stage,
     *           controller -> controller.setData(dados),
     *           controller -> controller.getResult());
     *
     *   // Fechar todos os modais
     *   NexusFX.dialogs().forceCloseAllModals();
     * </pre>
     */
    public static DialogOrchestrator dialogs() {
        if (bootstrap == null) {
            throw new IllegalStateException(
                    "❌ NexusFX não foi inicializado! Chame NexusFXBootstrap.run() primeiro.");
        }

        // Tenta obter do bootstrap primeiro
        DialogOrchestrator orchestrator = bootstrap.getDialogOrchestrator();

        if (orchestrator == null) {
            // Fallback via DiContainer
            try {
                DiContainer di = di();
                if (di != null) {
                    orchestrator = di.getBean(DialogOrchestrator.class);
                }
            } catch (Exception e) {
                throw new IllegalStateException(
                        "❌ DialogOrchestrator não disponível. Verifique a inicialização.", e);
            }
        }

        return orchestrator;
    }

    // ================================================================
    // 🏗️ DI CONTAINER - INJEÇÃO DE DEPENDÊNCIA
    // ================================================================

    /**
     * 🏗️ DiContainer - Injeção de dependência
     *
     * <pre>
     * Uso:
     *   // Obter bean por tipo
     *   MeuService service = NexusFX.di().getBean(MeuService.class);
     *
     *   // Obter bean por nome
     *   Object bean = NexusFX.di().getBean("meuBean");
     *
     *   // Registrar bean programaticamente
     *   NexusFX.di().register(Interface.class, Implementacao.class);
     * </pre>
     */
    public static DiContainer di() {
        if (bootstrap == null) {
            throw new IllegalStateException(
                    "❌ NexusFX não foi inicializado! Chame NexusFXBootstrap.run() primeiro.");
        }
        return bootstrap.getDiContainer();
    }

    // ================================================================
    // 🎭 STAGE PRINCIPAL
    // ================================================================

    /**
     * 🎭 Retorna o Stage principal da aplicação
     *
     * <pre>
     * Uso:
     *   Stage stage = NexusFX.stage();
     *   stage.setTitle("Minha Aplicação");
     * </pre>
     */
    public static Stage stage() {
        if (bootstrap == null) {
            throw new IllegalStateException(
                    "❌ NexusFX não foi inicializado! Chame NexusFXBootstrap.run() primeiro.");
        }
        return bootstrap.getPrimaryStage();
    }

    // ================================================================
    // 🔌 SHUTDOWN
    // ================================================================

    /**
     * 🔌 Desliga o framework completamente
     *
     * Fecha alertas, limpa caches, fecha recursos.
     */
    public static void shutdown() {
        if (bootstrap != null) {
            bootstrap.shutdown();
        }
    }

    // ================================================================
    // 🔍 DIAGNÓSTICO
    // ================================================================

    /**
     * 🔍 Diagnóstico rápido do framework
     * Exibe o status de todos os componentes no console
     */
    public static void diagnoseQuick() {
        System.out.println("\n🔍 NEXUS FX v8.0.4 - DIAGNÓSTICO RÁPIDO");
        System.out.println("=".repeat(50));

        boolean bootstrapOk = bootstrap != null;
        System.out.println("• Bootstrap: " + (bootstrapOk ? "✅ Vinculado" : "❌ Não vinculado"));
        System.out.println("• Inicializado: " + (bootstrapOk && bootstrap.isInitialized() ? "✅ Sim" : "❌ Não"));
        System.out.println("• ResourceAPI: " + (bootstrapOk && bootstrap.getResourceAPI() != null ? "✅ ATIVO" : "❌ INATIVO"));
        System.out.println("• ViewManager: " + (bootstrapOk && bootstrap.getViewManager() != null ? "✅ ATIVO" : "❌ INATIVO"));
        System.out.println("• ImageService: " + (bootstrapOk && bootstrap.getImageService() != null ? "✅ ATIVO" : "❌ INATIVO"));
        System.out.println("• AlertaSystem: " + (bootstrapOk && bootstrap.getAlertaSystem() != null ? "✅ ATIVO" : "❌ INATIVO"));
        System.out.println("• DialogOrchestrator: " + (bootstrapOk && bootstrap.getDialogOrchestrator() != null ? "✅ ATIVO" : "❌ INATIVO"));
        System.out.println("• DiContainer: " + (bootstrapOk && bootstrap.getDiContainer() != null ? "✅ ATIVO" : "❌ INATIVO"));

        if (bootstrapOk && bootstrap.getResourceAPI() != null) {
            ResourceAPI api = bootstrap.getResourceAPI();
            System.out.println("\n📊 RECURSOS REGISTRADOS:");
            System.out.println("   ├─ Total: " + api.count());
            System.out.println("   ├─ Views FXML: " + api.countByType(com.ossobo.winterfx.resources.enums.ResourceType.FXML));
            System.out.println("   ├─ Imagens: " + api.countByType(com.ossobo.winterfx.resources.enums.ResourceType.IMAGE));
            System.out.println("   ├─ CSS: " + api.countByType(com.ossobo.winterfx.resources.enums.ResourceType.CSS));
            System.out.println("   ├─ Sons: " + api.countByType(com.ossobo.winterfx.resources.enums.ResourceType.SOUND));
            System.out.println("   └─ Alertas: " + api.countByType(com.ossobo.winterfx.resources.enums.ResourceType.ALERT));
        }

        if (bootstrapOk && bootstrap.getViewManager() != null) {
            ViewManager vm = bootstrap.getViewManager();
            System.out.println("\n📊 VIEW MANAGER:");
            System.out.println("   ├─ Cache size: " + vm.getCacheSize());
            System.out.println("   └─ Cache hit rate: " + String.format(("%.1f%%" + vm.getCacheHitRate())));
        }

        System.out.println("=".repeat(50));
    }
}