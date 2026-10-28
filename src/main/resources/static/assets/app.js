(function () {
  "use strict";

  var PAGE_SIZE = 10;
  var session = readSession();
  var state = { status: "", priority: "", search: "", page: 0, pages: 0, total: 0, rows: [], users: [], editing: null, menu: null, mode: "login", toastTimer: null, searchTimer: null };
  var $ = function (selector) { return document.querySelector(selector); };
  var $$ = function (selector) { return Array.prototype.slice.call(document.querySelectorAll(selector)); };

  function readSession() {
    try { return JSON.parse(sessionStorage.getItem("dayline.session") || "null"); }
    catch (_) { return null; }
  }

  function saveSession(value) {
    session = value;
    sessionStorage.setItem("dayline.session", JSON.stringify(value));
  }

  function clearSession() {
    session = null;
    sessionStorage.removeItem("dayline.session");
  }

  function showAuth(message) {
    clearSession();
    $("#workspace").hidden = true;
    $("#auth-view").hidden = false;
    if (message) {
      $("#auth-error").textContent = message;
      $("#auth-error").hidden = false;
    }
  }

  function showWorkspace() {
    $("#auth-view").hidden = true;
    $("#workspace").hidden = false;
    $("#user-name").textContent = session.username || "User";
    $("#user-role").textContent = (session.role || "USER").replace("ROLE_", "").toUpperCase();
    $("#user-avatar").textContent = (session.username || "U").slice(0, 1).toUpperCase();
    var hour = new Date().getHours();
    $("#welcome-title").textContent = (hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening") + ", " + (session.username || "there") + ".";
    $("#today-label").textContent = new Intl.DateTimeFormat(undefined, { weekday: "short", month: "short", day: "numeric" }).format(new Date());
    $("#welcome-date").textContent = new Intl.DateTimeFormat(undefined, { weekday: "long", month: "long", day: "numeric" }).format(new Date()).toUpperCase();
    $("#assignee-field").hidden = !isAdmin();
    loadWorkspace();
  }

  function isAdmin() {
    return session && ["ADMIN", "ROLE_ADMIN"].indexOf(session.role) >= 0;
  }

  async function api(path, options) {
    options = options || {};
    var headers = new Headers(options.headers || {});
    if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    if (session && session.token) headers.set("Authorization", "Bearer " + session.token);
    var response;
    try {
      response = await fetch(path, Object.assign({}, options, { headers: headers }));
    } catch (_) {
      throw new Error("The server could not be reached. Check that the app is running and try again.");
    }
    var payload = null;
    var contentType = response.headers.get("content-type") || "";
    if (contentType.indexOf("application/json") >= 0) {
      try { payload = await response.json(); } catch (_) {}
    }
    if (!response.ok) {
      if (response.status === 401 && session) {
        clearSession();
        $("#workspace").hidden = true;
        $("#auth-view").hidden = false;
        $("#auth-error").textContent = "Your session expired. Sign in again to continue.";
        $("#auth-error").hidden = false;
      }
      throw new Error(payload && payload.message ? payload.message : "Request failed (" + response.status + ").");
    }
    return payload;
  }

  function setAuthMode(mode) {
    state.mode = mode;
    var registering = mode === "register";
    $("#auth-eyebrow").textContent = registering ? "A FRESH START" : "WELCOME BACK";
    $("#auth-title").textContent = registering ? "Create your workspace" : "Sign in to your workspace";
    $("#auth-subtitle").textContent = registering ? "A few details and you’re ready to begin." : "Pick up where you left off.";
    $("#username-label").firstChild.textContent = "Username";
    $("#username-label input").placeholder = registering ? "Choose a username" : "Your username";
    $("#email-label").hidden = !registering;
    $("#email-label input").required = registering;
    $("#auth-form [name=password]").autocomplete = registering ? "new-password" : "current-password";
    $("#auth-submit").innerHTML = registering ? "Create account <span aria-hidden=\"true\">→</span>" : "Sign in <span aria-hidden=\"true\">→</span>";
    $("#switch-copy").textContent = registering ? "Already have an account?" : "New to Dayline?";
    $("#auth-switch").textContent = registering ? "Sign in" : "Create an account";
    $("#auth-error").hidden = true;
  }

  $("#auth-switch").addEventListener("click", function () {
    setAuthMode(state.mode === "login" ? "register" : "login");
  });

  $("#auth-form").addEventListener("submit", async function (event) {
    event.preventDefault();
    var form = event.currentTarget;
    if (!form.reportValidity()) return;
    var values = new FormData(form);
    var registering = state.mode === "register";
    var body = { username: String(values.get("username")).trim(), password: values.get("password") };
    if (registering) body.email = String(values.get("email")).trim();
    var button = $("#auth-submit");
    button.disabled = true;
    $("#auth-error").hidden = true;
    try {
      var result = await api(registering ? "/auth/register" : "/auth/login", { method: "POST", body: JSON.stringify(body) });
      saveSession({ token: result.token, username: result.username, role: result.role });
      form.reset();
      showWorkspace();
    } catch (error) {
      $("#auth-error").textContent = error.message;
      $("#auth-error").hidden = false;
    } finally {
      button.disabled = false;
    }
  });

  $("#logout-button").addEventListener("click", function () {
    clearSession();
    $("#auth-form").reset();
    setAuthMode("login");
    showAuth();
  });

  function loadWorkspace() {
    loadTasks();
    loadStats();
    if (isAdmin()) loadUsers();
  }

  async function loadUsers() {
    try {
      state.users = await api("/users");
      populateAssignees();
    } catch (_) {
      state.users = [];
    }
  }

  function populateAssignees(selected) {
    var select = $("#task-form [name=assignedUserId]");
    var current = selected || select.value;
    while (select.options.length > 1) select.remove(1);
    state.users.forEach(function (user) {
      var option = document.createElement("option");
      option.value = user.id;
      option.textContent = user.username;
      select.appendChild(option);
    });
    if (current) select.value = String(current);
  }

  function queryString(values) {
    var query = new URLSearchParams();
    Object.keys(values).forEach(function (key) {
      var value = values[key];
      if (value !== "" && value !== null && value !== undefined) query.set(key, value);
    });
    return query.toString();
  }

  async function loadTasks() {
    $("#loading-state").hidden = false;
    $("#empty-state").hidden = true;
    $("#task-list").replaceChildren();
    var params = { page: state.page, size: PAGE_SIZE, sort: "createdAt,desc" };
    if (state.status) params.status = state.status;
    if (state.priority) params.priority = state.priority;
    if (state.search) params.search = state.search;
    try {
      var result = await api("/tasks?" + queryString(params));
      state.rows = result.content || [];
      state.pages = result.totalPages || 0;
      state.total = result.totalElements || 0;
      renderTasks();
      $("#list-count").textContent = state.total ? "(" + state.total + ")" : "";
      $("#page-summary").textContent = state.total === 0 ? "No tasks to show" : "Showing " + (state.page * PAGE_SIZE + 1) + "–" + Math.min((state.page + 1) * PAGE_SIZE, state.total) + " of " + state.total + " tasks";
      $("#page-number").textContent = (state.page + 1) + " / " + Math.max(1, state.pages);
      $("#prev-page").disabled = state.page <= 0;
      $("#next-page").disabled = state.pages === 0 || state.page >= state.pages - 1;
      $("#empty-state").hidden = state.rows.length > 0;
      $("#empty-title").textContent = state.search || state.status || state.priority ? "No matching tasks." : "A clean slate.";
      $("#empty-copy").textContent = state.search || state.status || state.priority ? "Try changing your search or filters." : "Add a task when something needs your attention.";
      $("#empty-new-button").hidden = Boolean(state.search || state.status || state.priority);
    } catch (error) {
      if (!session) return;
      showToast(error.message, true);
      $("#page-summary").textContent = "Could not load tasks";
    } finally {
      $("#loading-state").hidden = true;
    }
  }

  async function loadStats() {
    try {
      var responses = await Promise.all([
        api("/tasks?" + queryString({ page: 0, size: 1 })),
        api("/tasks?" + queryString({ page: 0, size: 1, status: "TODO" })),
        api("/tasks?" + queryString({ page: 0, size: 1, status: "IN_PROGRESS" })),
        api("/tasks?" + queryString({ page: 0, size: 1, status: "DONE" }))
      ]);
      var total = responses[0].totalElements || 0;
      var todo = responses[1].totalElements || 0;
      var progress = responses[2].totalElements || 0;
      var done = responses[3].totalElements || 0;
      $("#stat-total").textContent = total;
      $("#stat-open").textContent = todo + progress;
      $("#stat-progress").textContent = progress;
      $("#stat-done").textContent = done;
      $("#nav-all-count").textContent = total;
      $("#nav-todo-count").textContent = todo;
    } catch (_) {}
  }

  function makeBadge(text, cssClass) {
    var badge = document.createElement("span");
    badge.className = cssClass;
    badge.textContent = text;
    return badge;
  }

  function renderTasks() {
    var list = $("#task-list");
    list.replaceChildren();
    state.rows.forEach(function (task) {
      var row = document.createElement("article");
      row.className = "task-row" + (task.status === "DONE" ? " is-done" : "");
      var primary = document.createElement("div");
      primary.className = "task-primary";
      var toggle = document.createElement("button");
      toggle.className = "task-check" + (task.status === "DONE" ? " is-done" : "");
      toggle.type = "button";
      toggle.setAttribute("aria-label", task.status === "DONE" ? "Mark task incomplete" : "Mark task complete");
      toggle.textContent = task.status === "DONE" ? "✓" : "";
      toggle.addEventListener("click", function () {
        var nextStatus = task.status === "DONE" ? "TODO" : "DONE";
        updateTask(task, nextStatus);
      });
      var copy = document.createElement("div");
      copy.className = "task-copy";
      var title = document.createElement("div");
      title.className = "task-title";
      title.textContent = task.title || "Untitled task";
      var description = document.createElement("div");
      description.className = "task-description";
      description.textContent = task.description || (task.assignedTo && isAdmin() ? "Assigned to " + task.assignedTo.username : "No description");
      copy.append(title, description);
      primary.append(toggle, copy);

      var priorityCell = document.createElement("div");
      priorityCell.className = "priority-cell";
      var priority = (task.priority || "MEDIUM").toLowerCase();
      priorityCell.appendChild(makeBadge(capitalize(priority), "priority-badge priority-" + priority));

      var dueCell = document.createElement("div");
      dueCell.className = "due-cell due-date";
      if (task.dueDate) {
        dueCell.textContent = formatDate(task.dueDate);
        if (task.status !== "DONE" && task.dueDate < localToday()) dueCell.classList.add("overdue");
      } else {
        dueCell.textContent = "No date";
        dueCell.classList.add("no-date");
      }

      var statusCell = document.createElement("div");
      statusCell.className = "status-cell";
      var statusLabel = task.status === "IN_PROGRESS" ? "In progress" : task.status === "DONE" ? "Completed" : "To do";
      var statusClass = task.status === "IN_PROGRESS" ? "status-progress" : task.status === "DONE" ? "status-done" : "status-todo";
      statusCell.appendChild(makeBadge(statusLabel, "status-badge " + statusClass));

      var actions = document.createElement("div");
      actions.className = "row-actions";
      var menuButton = document.createElement("button");
      menuButton.className = "row-menu";
      menuButton.type = "button";
      menuButton.setAttribute("aria-label", "Task actions");
      menuButton.textContent = "···";
      var menu = document.createElement("div");
      menu.className = "row-menu-popover";
      menu.hidden = true;
      var edit = document.createElement("button");
      edit.type = "button";
      edit.textContent = "Edit task";
      edit.addEventListener("click", function () { closeMenus(); openTaskDialog(task); });
      var remove = document.createElement("button");
      remove.type = "button";
      remove.className = "danger";
      remove.textContent = "Delete task";
      remove.addEventListener("click", function () { deleteTask(task); });
      menu.append(edit, remove);
      menuButton.addEventListener("click", function (event) {
        event.stopPropagation();
        var wasHidden = menu.hidden;
        closeMenus();
        menu.hidden = !wasHidden;
        state.menu = menu.hidden ? null : menu;
      });
      actions.append(menuButton, menu);

      row.append(primary, priorityCell, dueCell, statusCell, actions);
      list.appendChild(row);
    });
  }

  function closeMenus() {
    if (state.menu) state.menu.hidden = true;
    state.menu = null;
  }

  document.addEventListener("click", closeMenus);
  $("#prev-page").addEventListener("click", function () { if (state.page > 0) { state.page--; loadTasks(); } });
  $("#next-page").addEventListener("click", function () { if (state.page + 1 < state.pages) { state.page++; loadTasks(); } });

  $$(".nav-item").forEach(function (button) {
    button.addEventListener("click", function () {
      $$(".nav-item").forEach(function (item) { item.classList.remove("active"); });
      button.classList.add("active");
      state.status = button.dataset.status || "";
      state.page = 0;
      $("#view-label").textContent = button.querySelector("span:nth-child(2)").textContent;
      $("#list-title").firstChild.textContent = button.querySelector("span:nth-child(2)").textContent + " ";
      loadTasks();
    });
  });

  $("#priority-filter").addEventListener("change", function (event) {
    state.priority = event.target.value;
    state.page = 0;
    loadTasks();
  });

  $("#search-input").addEventListener("input", function (event) {
    clearTimeout(state.searchTimer);
    state.searchTimer = setTimeout(function () {
      state.search = event.target.value.trim();
      state.page = 0;
      loadTasks();
    }, 260);
  });

  $("#new-task-button").addEventListener("click", function () { openTaskDialog(null); });
  $("#empty-new-button").addEventListener("click", function () { openTaskDialog(null); });
  $("#close-dialog").addEventListener("click", closeTaskDialog);
  $("#cancel-task").addEventListener("click", closeTaskDialog);

  function openTaskDialog(task) {
    state.editing = task;
    var form = $("#task-form");
    form.reset();
    $("#task-error").hidden = true;
    var editing = Boolean(task);
    $("#dialog-title").textContent = editing ? "Edit task" : "Create a task";
    $("#save-task").textContent = editing ? "Save changes" : "Create task";
    form.elements.title.value = editing ? task.title : "";
    form.elements.description.value = editing ? task.description || "" : "";
    form.elements.priority.value = editing ? task.priority || "MEDIUM" : "MEDIUM";
    form.elements.status.value = editing ? task.status || "TODO" : "TODO";
    form.elements.dueDate.value = editing ? task.dueDate || "" : "";
    if (isAdmin()) populateAssignees(editing && task.assignedTo ? task.assignedTo.id : "");
    $("#task-dialog").showModal();
    setTimeout(function () { form.elements.title.focus(); }, 0);
  }

  function closeTaskDialog() {
    if ($("#task-dialog").open) $("#task-dialog").close();
    state.editing = null;
  }

  $("#task-form").addEventListener("submit", async function (event) {
    event.preventDefault();
    var form = event.currentTarget;
    if (!form.reportValidity()) return;
    var values = new FormData(form);
    var payload = {
      title: String(values.get("title")).trim(),
      description: String(values.get("description")).trim() || null,
      priority: values.get("priority"),
      status: values.get("status"),
      dueDate: values.get("dueDate") || null
    };
    if (isAdmin()) payload.assignedUserId = values.get("assignedUserId") ? Number(values.get("assignedUserId")) : null;
    var editing = state.editing;
    var button = $("#save-task");
    button.disabled = true;
    $("#task-error").hidden = true;
    try {
      await api(editing ? "/tasks/" + editing.id : "/tasks", { method: editing ? "PUT" : "POST", body: JSON.stringify(payload) });
      closeTaskDialog();
      showToast(editing ? "Task updated." : "Task created.");
      await Promise.all([loadTasks(), loadStats()]);
    } catch (error) {
      $("#task-error").textContent = error.message;
      $("#task-error").hidden = false;
    } finally {
      button.disabled = false;
    }
  });

  async function updateTask(task, status) {
    var payload = { title: task.title, description: task.description, priority: task.priority, status: status, dueDate: task.dueDate };
    if (isAdmin()) payload.assignedUserId = task.assignedTo ? task.assignedTo.id : null;
    try {
      await api("/tasks/" + task.id, { method: "PUT", body: JSON.stringify(payload) });
      showToast(status === "DONE" ? "Task completed." : "Task reopened.");
      await Promise.all([loadTasks(), loadStats()]);
    } catch (error) {
      showToast(error.message, true);
    }
  }

  async function deleteTask(task) {
    closeMenus();
    if (!window.confirm("Delete “" + task.title + "”? This cannot be undone.")) return;
    try {
      await api("/tasks/" + task.id, { method: "DELETE" });
      if (state.rows.length === 1 && state.page > 0) state.page--;
      showToast("Task deleted.");
      await Promise.all([loadTasks(), loadStats()]);
    } catch (error) {
      showToast(error.message, true);
    }
  }

  function showToast(message, isError) {
    var toast = $("#toast");
    toast.textContent = message;
    toast.classList.toggle("error", Boolean(isError));
    toast.classList.add("show");
    clearTimeout(state.toastTimer);
    state.toastTimer = setTimeout(function () { toast.classList.remove("show"); }, 3200);
  }

  function capitalize(value) { return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase(); }
  function localToday() {
    var date = new Date();
    return date.getFullYear() + "-" + String(date.getMonth() + 1).padStart(2, "0") + "-" + String(date.getDate()).padStart(2, "0");
  }
  function formatDate(value) {
    var parts = value.split("-").map(Number);
    var date = new Date(parts[0], parts[1] - 1, parts[2]);
    return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric", year: parts[0] === new Date().getFullYear() ? undefined : "numeric" }).format(date);
  }

  if (session && session.token) showWorkspace();
  else showAuth();
})();
