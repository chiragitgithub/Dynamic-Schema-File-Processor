<script>
document.getElementById("exportBtn").addEventListener("click", function () {
    const baseUrl = "http://localhost:8080"; // Change if backend is deployed elsewhere

    const tableName = document.getElementById("tableName").value.trim();
    const primaryKey = document.getElementById("primaryKey")?.value.trim(); // Optional field
    const id = document.getElementById("ids").value.trim();
    const page = document.getElementById("page").value.trim() || 0;
    const size = document.getElementById("size").value.trim() || 10;
    const format = document.getElementById("format").value.trim();

    if (!tableName || !format || (id && !primaryKey)) {
        alert("Please provide table name, export format, and primary key if ID(s) are specified.");
        return;
    }

    let url = `${baseUrl}/api/export?tableName=${encodeURIComponent(tableName)}&format=${encodeURIComponent(format)}&page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}`;

    if (id) {
        url += `&id=${encodeURIComponent(id)}&primaryKey=${encodeURIComponent(primaryKey)}`;
    }

    console.log("Requesting export from:", url);

    fetch(url)
        .then(response => {
            console.log("Response status:", response.status);
            const contentType = response.headers.get("Content-Type");
            console.log("Content-Type:", contentType);

            if (!response.ok) {
                throw new Error("Export failed. Server returned status " + response.status);
            }

            return response.blob();
        })
        .then(blob => {
            console.log("Blob received. Size:", blob.size);
            const downloadUrl = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            const extension = format === 'excel' ? 'xlsx' : (format === 'json' ? 'json' : 'csv');
            a.href = downloadUrl;
            a.download = `${tableName}_export.${extension}`;
            document.body.appendChild(a);
            a.click();
            a.remove();
        })
        .catch(error => {
            console.error("Export error:", error);
            alert("Export failed: " + error.message);
        });
});
</script>
