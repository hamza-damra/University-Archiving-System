export function getStatusColor(status) {
    const statusMap = {
        'PENDING': 'yellow',
        'IN_PROGRESS': 'blue',
        'COMPLETED': 'green',
        'OVERDUE': 'red',
        'APPROVED': 'emerald',
        'REJECTED': 'rose'
    };
    return statusMap[status] || 'gray';
}

export function initializeDropdowns() {
    document.querySelectorAll('[data-dropdown-trigger]').forEach(trigger => {
        trigger.addEventListener('click', (e) => {
            e.stopPropagation();
            const targetId = trigger.getAttribute('data-dropdown-trigger');
            const dropdown = document.getElementById(targetId);

            // Close all other dropdowns
            document.querySelectorAll('[id^="task-menu-"]').forEach(d => {
                if (d.id !== targetId) d.classList.add('hidden');
            });

            dropdown.classList.toggle('hidden');
        });
    });

    // Close on click outside
    document.addEventListener('click', () => {
        document.querySelectorAll('[id^="task-menu-"]').forEach(d => d.classList.add('hidden'));
    }, { once: true });
}
